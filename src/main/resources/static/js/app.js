const App = (() => {
    const DIAS_PT = {
        MONDAY: 'Segunda-feira',
        TUESDAY: 'Terça-feira',
        WEDNESDAY: 'Quarta-feira',
        THURSDAY: 'Quinta-feira',
        FRIDAY: 'Sexta-feira',
        SATURDAY: 'Sábado',
        SUNDAY: 'Domingo'
    };

    let ownerToken = localStorage.getItem('pp_owner_token') || null;
    let chatConversaId = null;
    let modoVisaoAgenda = 'DIA'; // DIA ou SEMANA
    let conversaAdminSelecionada = null;

    function hojeIso() {
        const d = new Date();
        const off = d.getTimezoneOffset();
        return new Date(d.getTime() - off * 60000).toISOString().slice(0, 10);
    }

    function amanhaUtilIso() {
        const d = new Date();
        d.setDate(d.getDate() + 1);
        if (d.getDay() === 0) d.setDate(d.getDate() + 1);
        const off = d.getTimezoneOffset();
        return new Date(d.getTime() - off * 60000).toISOString().slice(0, 10);
    }

    function formatarDataBr(isoDate) {
        if (!isoDate) return '';
        const [y, m, d] = isoDate.split('-');
        return `${d}/${m}/${y}`;
    }

    function escapeHtml(str) {
        if (str == null) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function showToast(msg, isError = false) {
        const box = document.getElementById('toastBox');
        box.textContent = msg;
        box.className = isError ? 'toast error' : 'toast';
        clearTimeout(box._timer);
        box._timer = setTimeout(() => box.classList.add('hidden'), 4500);
    }

    async function api(url, options = {}) {
        const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
        if (ownerToken && url.startsWith('/api/admin')) {
            headers['Authorization'] = `Bearer ${ownerToken}`;
        }
        const resp = await fetch(url, { ...options, headers });
        if (resp.status === 204) return null;
        const data = await resp.json().catch(() => ({}));
        if (!resp.ok) {
            if (resp.status === 401 && url.startsWith('/api/admin') && !url.endsWith('/login')) {
                ownerToken = null;
                localStorage.removeItem('pp_owner_token');
                atualizarEstadoLoginOwner();
            }
            throw new Error(data.mensagem || `Erro HTTP ${resp.status}`);
        }
        return data;
    }

    function switchView(viewName) {
        document.querySelectorAll('.mode-btn').forEach(b => {
            b.classList.toggle('active', b.dataset.view === viewName);
        });
        document.querySelectorAll('.view-section').forEach(s => {
            s.classList.toggle('active', s.id === `view-${viewName}`);
        });
        if (viewName === 'proprietario') {
            atualizarEstadoLoginOwner();
        }
    }

    async function carregarCatalogoInicial() {
        const cat = await api('/api/publico/catalogo');
        const selServico = document.getElementById('agTipoServico');
        selServico.innerHTML = cat.tiposServico
            .map(s => `<option value="${s.codigo}">${escapeHtml(s.descricao)}</option>`)
            .join('');

        const selPerfil = document.getElementById('agPerfil');
        selPerfil.innerHTML = cat.perfisCliente
            .map(p => `<option value="${p.codigo}">${escapeHtml(p.descricao)}</option>`)
            .join('');

        const inputData = document.getElementById('agData');
        inputData.min = hojeIso();
        inputData.value = amanhaUtilIso();
        await carregarHorariosLivresCliente();
    }

    async function carregarHorariosLivresCliente() {
        const data = document.getElementById('agData').value;
        const container = document.getElementById('slotsContainer');
        const hiddenHora = document.getElementById('agHoraInicio');
        hiddenHora.value = '';

        if (!data) {
            container.innerHTML = '<span class="muted">Escolha uma data acima.</span>';
            return;
        }
        try {
            const res = await api(`/api/publico/horarios-livres?data=${data}`);
            if (!res.horariosLivres || res.horariosLivres.length === 0) {
                container.innerHTML = '<div class="empty-state" style="width:100%">Nenhum horário livre nesta data (fora de expediente, feriado bloqueado ou agenda lotada). Tente outra data!</div>';
                return;
            }
            container.innerHTML = res.horariosLivres
                .map(h => `<button type="button" class="slot-btn" data-hora="${h}">${h}</button>`)
                .join('');

            container.querySelectorAll('.slot-btn').forEach(btn => {
                btn.addEventListener('click', () => {
                    container.querySelectorAll('.slot-btn').forEach(b => b.classList.remove('selected'));
                    btn.classList.add('selected');
                    hiddenHora.value = btn.dataset.hora;
                });
            });
        } catch (e) {
            container.innerHTML = `<span class="muted">${escapeHtml(e.message)}</span>`;
        }
    }

    async function submeterNovoAgendamento(ev) {
        ev.preventDefault();
        const horaInicio = document.getElementById('agHoraInicio').value;
        if (!horaInicio) {
            showToast('Selecione um dos horários livres disponíveis antes de confirmar.', true);
            return;
        }
        const payload = {
            nomeCliente: document.getElementById('agNome').value,
            telefone: document.getElementById('agTelefone').value,
            email: document.getElementById('agEmail').value,
            endereco: document.getElementById('agEndereco').value,
            perfilCliente: document.getElementById('agPerfil').value,
            data: document.getElementById('agData').value,
            horaInicio: horaInicio,
            tipoServico: document.getElementById('agTipoServico').value,
            descricaoServico: document.getElementById('agDescricao').value
        };

        try {
            const criado = await api('/api/publico/agendamentos', {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            document.getElementById('confProtocolo').textContent = criado.protocolo;
            document.getElementById('confResumoLista').innerHTML = `
                <li><strong>Cliente:</strong> ${escapeHtml(criado.clienteNome)} (${escapeHtml(criado.clientePerfil)})</li>
                <li><strong>Data e Horário:</strong> ${formatarDataBr(criado.data)} das ${criado.horaInicio} às ${criado.horaFim}</li>
                <li><strong>Serviço:</strong> ${escapeHtml(criado.tipoServicoDescricao)}</li>
                <li><strong>Local:</strong> ${escapeHtml(criado.enderecoAtendimento)}</li>
            `;
            document.getElementById('cardConfirmacaoRecente').classList.remove('hidden');
            showToast(`Agendamento confirmado! Protocolo ${criado.protocolo}`);
            await carregarHorariosLivresCliente();
        } catch (e) {
            showToast(e.message, true);
        }
    }

    function renderizarCardAgendamentoCliente(a, historico = null) {
        const podeAlterar = a.statusCodigo === 'CONFIRMADO';
        const histHtml = historico
            ? `<div style="margin-top:8px;font-size:0.8rem;color:#475569">
                <strong>Histórico do protocolo:</strong>
                ${historico.map(h => `<div>• ${escapeHtml(h.statusNovo)} por ${escapeHtml(h.responsavel)} (${escapeHtml(h.observacao || '')})</div>`).join('')}
               </div>`
            : '';

        return `
            <div class="appointment-item st-${a.statusCodigo}">
                <div class="appointment-top">
                    <strong>${escapeHtml(a.protocolo)} • ${formatarDataBr(a.data)} às ${a.horaInicio}</strong>
                    <span class="badge status-${a.statusCodigo}">${escapeHtml(a.statusDescricao)}</span>
                </div>
                <div><strong>Serviço:</strong> ${escapeHtml(a.tipoServicoDescricao)}</div>
                <div><strong>Cliente:</strong> ${escapeHtml(a.clienteNome)} (${escapeHtml(a.clientePerfil)})</div>
                <div><strong>Endereço:</strong> ${escapeHtml(a.enderecoAtendimento)}</div>
                ${a.observacaoTecnica ? `<div><strong>Registro técnico:</strong> ${escapeHtml(a.observacaoTecnica)}</div>` : ''}
                ${histHtml}
                ${podeAlterar ? `
                    <div class="appointment-actions">
                        <button type="button" class="btn btn-sm btn-outline" onclick="App.abrirModalRemarcar('${a.protocolo}', '${a.data}')">Remarcar Horário</button>
                        <button type="button" class="btn btn-sm btn-danger" onclick="App.cancelarPeloCliente('${a.protocolo}')">Cancelar Visita</button>
                    </div>
                ` : ''}
            </div>
        `;
    }

    async function consultarProtocoloCliente() {
        const prot = document.getElementById('inputBuscaProtocolo').value.trim();
        const box = document.getElementById('resultadoConsultaCliente');
        if (!prot) return showToast('Digite o código de protocolo.', true);
        try {
            const res = await api(`/api/publico/agendamentos/protocolo/${encodeURIComponent(prot)}`);
            box.innerHTML = renderizarCardAgendamentoCliente(res.agendamento, res.historico);
        } catch (e) {
            box.innerHTML = `<div class="empty-state">${escapeHtml(e.message)}</div>`;
        }
    }

    async function consultarTelefoneCliente() {
        const tel = document.getElementById('inputBuscaTelefone').value.trim();
        const box = document.getElementById('resultadoConsultaCliente');
        if (!tel) return showToast('Digite o telefone com DDD.', true);
        try {
            const lista = await api(`/api/publico/agendamentos/cliente?telefone=${encodeURIComponent(tel)}`);
            if (lista.length === 0) {
                box.innerHTML = '<div class="empty-state">Nenhum agendamento encontrado para este telefone.</div>';
                return;
            }
            box.innerHTML = lista.map(a => renderizarCardAgendamentoCliente(a)).join('');
        } catch (e) {
            box.innerHTML = `<div class="empty-state">${escapeHtml(e.message)}</div>`;
        }
    }

    async function abrirModalRemarcar(protocolo, dataAtual) {
        document.getElementById('remProtocolo').value = protocolo;
        document.getElementById('remProtocoloTitulo').textContent = protocolo;
        const inputData = document.getElementById('remNovaData');
        inputData.min = hojeIso();
        inputData.value = dataAtual >= hojeIso() ? dataAtual : amanhaUtilIso();
        await atualizarHorariosModalRemarcar();
        document.getElementById('modalRemarcar').classList.remove('hidden');
    }

    function fecharModalRemarcar() {
        document.getElementById('modalRemarcar').classList.add('hidden');
    }

    async function atualizarHorariosModalRemarcar() {
        const data = document.getElementById('remNovaData').value;
        const sel = document.getElementById('remNovoHorario');
        const res = await api(`/api/publico/horarios-livres?data=${data}`);
        if (!res.horariosLivres.length) {
            sel.innerHTML = '<option value="">Sem horários livres nesta data</option>';
        } else {
            sel.innerHTML = res.horariosLivres.map(h => `<option value="${h}">${h}</option>`).join('');
        }
    }

    async function confirmarRemarcacaoCliente(ev) {
        ev.preventDefault();
        const protocolo = document.getElementById('remProtocolo').value;
        const novaData = document.getElementById('remNovaData').value;
        const novaHoraInicio = document.getElementById('remNovoHorario').value;
        const motivo = document.getElementById('remMotivo').value;
        try {
            await api(`/api/publico/agendamentos/${encodeURIComponent(protocolo)}/remarcar`, {
                method: 'PUT',
                body: JSON.stringify({ novaData, novaHoraInicio, motivo })
            });
            fecharModalRemarcar();
            showToast(`Protocolo ${protocolo} remarcado com sucesso!`);
            document.getElementById('inputBuscaProtocolo').value = protocolo;
            await consultarProtocoloCliente();
        } catch (e) {
            showToast(e.message, true);
        }
    }

    async function cancelarPeloCliente(protocolo) {
        const motivo = prompt(`Informe opcionalmente o motivo do cancelamento do protocolo ${protocolo}:`, 'Imprevisto pessoal');
        if (motivo === null) return;
        try {
            await api(`/api/publico/agendamentos/${encodeURIComponent(protocolo)}/cancelar`, {
                method: 'POST',
                body: JSON.stringify({ motivo })
            });
            showToast(`Agendamento ${protocolo} cancelado e horário liberado na agenda.`);
            document.getElementById('inputBuscaProtocolo').value = protocolo;
            await consultarProtocoloCliente();
        } catch (e) {
            showToast(e.message, true);
        }
    }

    /* ==================== CHAT DO CLIENTE ==================== */
    function renderizarMensagensChat(conversa) {
        chatConversaId = conversa.id;
        const badge = document.getElementById('chatConversaBadge');
        badge.textContent = conversa.statusDescricao;
        badge.className = `badge status-${conversa.statusCodigo}`;

        const box = document.getElementById('chatMessagesBox');
        box.innerHTML = conversa.mensagens.map(m => {
            const cls = m.remetente === 'CLIENTE' ? 'msg-user' : (m.remetente === 'PROPRIETARIO' ? 'msg-owner' : 'msg-bot');
            const autor = m.remetente === 'CLIENTE' ? 'Você' : (m.remetente === 'PROPRIETARIO' ? 'Carlos (Proprietário Power Plus)' : 'Assistente Power Plus');
            return `
                <div class="msg ${cls}">
                    <div class="msg-meta">${autor}</div>
                    <div class="msg-bubble">${escapeHtml(m.conteudo)}</div>
                </div>
            `;
        }).join('');
        box.scrollTop = box.scrollHeight;
    }

    async function enviarMensagemChat(texto, solicitarHumano = false) {
        const nomeCliente = document.getElementById('chatNomeCliente').value || 'Cliente Web';
        const telefoneCliente = document.getElementById('chatTelefoneCliente').value || '';
        try {
            const conv = await api('/api/publico/chat/mensagens', {
                method: 'POST',
                body: JSON.stringify({
                    conversaId: chatConversaId,
                    nomeCliente,
                    telefoneCliente,
                    mensagem: texto,
                    solicitarHumano
                })
            });
            renderizarMensagensChat(conv);
        } catch (e) {
            showToast(e.message, true);
        }
    }

    /* ==================== PAINEL DO PROPRIETÁRIO ==================== */
    function atualizarEstadoLoginOwner() {
        const logado = Boolean(ownerToken);
        document.getElementById('ownerLoginBox').classList.toggle('hidden', logado);
        document.getElementById('ownerAppBox').classList.toggle('hidden', !logado);
        if (logado) {
            carregarTudoPainelProprietario();
        }
    }

    async function loginProprietario(ev) {
        ev.preventDefault();
        try {
            const res = await api('/api/admin/auth/login', {
                method: 'POST',
                body: JSON.stringify({
                    username: document.getElementById('loginUser').value,
                    password: document.getElementById('loginPass').value
                })
            });
            ownerToken = res.token;
            localStorage.setItem('pp_owner_token', ownerToken);
            document.getElementById('ownerWelcomeTitle').textContent = `Painel Operacional – ${res.nomeExibicao}`;
            showToast('Autenticação realizada com sucesso!');
            atualizarEstadoLoginOwner();
        } catch (e) {
            showToast(e.message, true);
        }
    }

    async function carregarTudoPainelProprietario() {
        const campoData = document.getElementById('adminDataAgenda');
        if (!campoData.value) campoData.value = hojeIso();
        await Promise.all([
            carregarDashboardEAgenda(),
            carregarDisponibilidadeAdmin(),
            carregarClientesAdmin(),
            carregarConversasAdmin(),
            carregarNotificacoesAdmin()
        ]);
    }

    async function carregarDashboardEAgenda() {
        const dataRef = document.getElementById('adminDataAgenda').value || hojeIso();
        const dash = await api(`/api/admin/dashboard?data=${dataRef}`);

        document.getElementById('kpiTotalHoje').textContent = dash.totalHoje;
        document.getElementById('kpiConfirmadosHoje').textContent = dash.confirmadosHoje + dash.emAtendimentoHoje;
        document.getElementById('kpiConcluidosHoje').textContent = dash.concluidosHoje;
        document.getElementById('kpiTotalSemana').textContent = dash.totalSemana;
        document.getElementById('countChatsPendentes').textContent = dash.chatsAguardandoHumano;
        document.getElementById('countNotifNaoLidas').textContent = dash.notificacoesNaoLidas;

        const alertaHeader = document.getElementById('badgeOwnerAlert');
        alertaHeader.classList.toggle('hidden', (dash.chatsAguardandoHumano + dash.notificacoesNaoLidas) === 0);

        document.getElementById('ownerFeatureFlagInfo').textContent = dash.whatsappFeatureFlagAtiva
            ? 'Integração WhatsApp Externo: ATIVA (Feature Flag ON)'
            : 'Canal de Notificações: Sistema Interno Ativo | API WhatsApp Externa: Desativada via Feature Flag (Escopo MVP)';

        const container = document.getElementById('adminAgendaContainer');
        if (modoVisaoAgenda === 'DIA') {
            if (dash.atendimentosHoje.length === 0) {
                container.innerHTML = `<div class="empty-state">Agenda livre em ${formatarDataBr(dataRef)}! Nenhum atendimento marcado.</div>`;
                return;
            }
            container.innerHTML = dash.atendimentosHoje.map(renderizarCardAgendaAdmin).join('');
        } else {
            const sem = await api(`/api/admin/agenda/semana?dataReferencia=${dataRef}`);
            const porData = {};
            sem.agendamentos.forEach(a => {
                porData[a.data] = porData[a.data] || [];
                porData[a.data].push(a);
            });

            let cols = '';
            let cursor = new Date(sem.inicioSemana + 'T12:00:00');
            for (let i = 0; i < 6; i++) {
                const iso = cursor.toISOString().slice(0, 10);
                const itens = porData[iso] || [];
                cols += `
                    <div class="week-col">
                        <h4>${formatarDataBr(iso)}</h4>
                        ${itens.length === 0
                            ? '<span class="muted" style="font-size:0.8rem">Livre</span>'
                            : itens.map(a => `
                                <div class="appointment-item st-${a.statusCodigo}" style="padding:9px;margin-bottom:8px">
                                    <div><strong>${a.horaInicio}</strong> • <span class="badge status-${a.statusCodigo}">${escapeHtml(a.statusDescricao)}</span></div>
                                    <div style="font-size:0.83rem;font-weight:600">${escapeHtml(a.clienteNome)}</div>
                                    <div style="font-size:0.78rem;color:#475569">${escapeHtml(a.tipoServicoDescricao)}</div>
                                </div>
                              `).join('')}
                    </div>
                `;
                cursor.setDate(cursor.getDate() + 1);
            }
            container.innerHTML = `<div class="week-grid">${cols}</div>`;
        }
    }

    function renderizarCardAgendaAdmin(a) {
        let botoes = '';
        if (a.statusCodigo === 'CONFIRMADO') {
            botoes += `<button class="btn btn-sm btn-primary" onclick="App.alterarStatusAdmin(${a.id}, 'EM_ATENDIMENTO')">Iniciar Serviço</button>`;
            botoes += `<button class="btn btn-sm btn-secondary" onclick="App.alterarStatusAdmin(${a.id}, 'CONCLUIDO')">Concluir Direto</button>`;
            botoes += `<button class="btn btn-sm btn-outline" onclick="App.enviarLembreteAdmin(${a.id})">Enviar Lembrete</button>`;
            botoes += `<button class="btn btn-sm btn-danger" onclick="App.alterarStatusAdmin(${a.id}, 'CANCELADO')">Cancelar</button>`;
        } else if (a.statusCodigo === 'EM_ATENDIMENTO') {
            botoes += `<button class="btn btn-sm btn-secondary" onclick="App.alterarStatusAdmin(${a.id}, 'CONCLUIDO')">Concluir Serviço</button>`;
            botoes += `<button class="btn btn-sm btn-danger" onclick="App.alterarStatusAdmin(${a.id}, 'CANCELADO')">Cancelar</button>`;
        }

        return `
            <div class="appointment-item st-${a.statusCodigo}">
                <div class="appointment-top">
                    <strong>${a.horaInicio}–${a.horaFim} | ${escapeHtml(a.clienteNome)} (${escapeHtml(a.clientePerfil)})</strong>
                    <div>
                        <span class="muted" style="font-size:0.8rem;margin-right:6px">${escapeHtml(a.protocolo)}</span>
                        <span class="badge status-${a.statusCodigo}">${escapeHtml(a.statusDescricao)}</span>
                    </div>
                </div>
                <div><strong>Serviço:</strong> ${escapeHtml(a.tipoServicoDescricao)}</div>
                <div><strong>Endereço:</strong> ${escapeHtml(a.enderecoAtendimento)} | <strong>Tel:</strong> ${escapeHtml(a.clienteTelefone)}</div>
                ${a.descricaoServico ? `<div><strong>Relato do cliente:</strong> ${escapeHtml(a.descricaoServico)}</div>` : ''}
                ${a.observacaoTecnica ? `<div><strong>Nota técnica:</strong> ${escapeHtml(a.observacaoTecnica)}</div>` : ''}
                <div class="appointment-actions">${botoes}</div>
            </div>
        `;
    }

    async function alterarStatusAdmin(id, novoStatus) {
        let observacao = '';
        if (novoStatus === 'CONCLUIDO') {
            observacao = prompt('Observação técnica do serviço concluído (opcional):', 'Serviço executado e testado conforme normas.') || '';
        } else if (novoStatus === 'CANCELADO') {
            observacao = prompt('Motivo do cancelamento:', 'Solicitação operacional') || '';
        }
        try {
            await api(`/api/admin/agendamentos/${id}/status`, {
                method: 'PATCH',
                body: JSON.stringify({ novoStatus, observacao })
            });
            showToast('Status do atendimento atualizado!');
            await carregarDashboardEAgenda();
            await carregarNotificacoesAdmin();
        } catch (e) {
            showToast(e.message, true);
        }
    }

    async function enviarLembreteAdmin(id) {
        try {
            const res = await api(`/api/admin/agendamentos/${id}/lembrete`, { method: 'POST' });
            showToast(res.mensagem);
            await carregarNotificacoesAdmin();
        } catch (e) {
            showToast(e.message, true);
        }
    }

    async function carregarDisponibilidadeAdmin() {
        const cfg = await api('/api/admin/disponibilidade');
        const box = document.getElementById('listaDisponibilidadeSemanal');
        box.innerHTML = cfg.disponibilidades.map(d => `
            <div class="disp-row">
                <strong>${DIAS_PT[d.diaSemana] || d.diaSemana}</strong>
                <label><input type="checkbox" id="chk_${d.diaSemana}" ${d.ativo ? 'checked' : ''} style="width:auto"> Ativo</label>
                <input type="time" id="ini_${d.diaSemana}" value="${d.horaInicio.slice(0,5)}">
                <input type="time" id="fim_${d.diaSemana}" value="${d.horaFim.slice(0,5)}">
                <button type="button" class="btn btn-sm btn-outline" onclick="App.salvarDiaDisponibilidade('${d.diaSemana}', ${d.duracaoSlotMinutos})">Salvar</button>
            </div>
        `).join('');

        const bloqBox = document.getElementById('listaBloqueiosAtivos');
        bloqBox.innerHTML = cfg.bloqueios.length === 0
            ? '<div class="empty-state">Nenhuma data bloqueada.</div>'
            : cfg.bloqueios.map(b => `
                <div class="appointment-item" style="display:flex;justify-content:space-between;align-items:center;padding:10px 14px">
                    <div><strong>${formatarDataBr(b.data)}</strong> — ${escapeHtml(b.motivo)}</div>
                    <button type="button" class="btn btn-sm btn-danger" onclick="App.removerBloqueioAdmin(${b.id})">Desbloquear</button>
                </div>
            `).join('');
    }

    async function salvarDiaDisponibilidade(diaSemana, duracaoSlotMinutos) {
        const ativo = document.getElementById(`chk_${diaSemana}`).checked;
        const horaInicio = document.getElementById(`ini_${diaSemana}`).value;
        const horaFim = document.getElementById(`fim_${diaSemana}`).value;
        try {
            await api('/api/admin/disponibilidade', {
                method: 'PUT',
                body: JSON.stringify({ diaSemana, ativo, horaInicio, horaFim, duracaoSlotMinutos })
            });
            showToast(`Disponibilidade de ${DIAS_PT[diaSemana]} atualizada!`);
            await carregarHorariosLivresCliente();
        } catch (e) {
            showToast(e.message, true);
        }
    }

    async function criarBloqueioAdmin(ev, forcar = false) {
        if (ev) ev.preventDefault();
        const data = document.getElementById('bloqData').value;
        const motivo = document.getElementById('bloqMotivo').value;
        try {
            await api('/api/admin/bloqueios', {
                method: 'POST',
                body: JSON.stringify({ data, motivo, forcarComConflito: forcar })
            });
            showToast('Data bloqueada com sucesso na agenda!');
            document.getElementById('formNovoBloqueio').reset();
            await carregarDisponibilidadeAdmin();
            await carregarHorariosLivresCliente();
        } catch (e) {
            if (!forcar && e.message.includes('Confirme o bloqueio')) {
                if (confirm(`${e.message}\n\nDeseja confirmar o bloqueio mesmo assim?`)) {
                    return criarBloqueioAdmin(null, true);
                }
            } else {
                showToast(e.message, true);
            }
        }
    }

    async function removerBloqueioAdmin(id) {
        await api(`/api/admin/bloqueios/${id}`, { method: 'DELETE' });
        showToast('Bloqueio removido.');
        await carregarDisponibilidadeAdmin();
    }

    async function carregarClientesAdmin() {
        const termo = document.getElementById('inputAdminBuscaCliente').value.trim();
        const url = termo ? `/api/admin/clientes?termo=${encodeURIComponent(termo)}` : '/api/admin/clientes';
        const lista = await api(url);
        const box = document.getElementById('listaAdminClientes');
        box.innerHTML = lista.length === 0
            ? '<div class="empty-state">Nenhum cliente encontrado.</div>'
            : lista.map(c => `
                <div class="appointment-item" style="padding:11px 14px">
                    <div class="appointment-top">
                        <strong>${escapeHtml(c.nome)}</strong>
                        <span class="badge status-CONFIRMADO">${escapeHtml(c.perfilDescricao)}</span>
                    </div>
                    <div style="font-size:0.85rem">Tel: ${escapeHtml(c.telefone)} | ${escapeHtml(c.enderecoPadrao)}</div>
                    <div class="appointment-actions">
                        <button type="button" class="btn btn-sm btn-outline" onclick="App.verHistoricoClienteAdmin(${c.id}, '${escapeHtml(c.nome)}')">Ver Histórico de Serviços</button>
                    </div>
                </div>
            `).join('');
    }

    async function cadastrarClienteAdmin(ev) {
        ev.preventDefault();
        try {
            await api('/api/admin/clientes', {
                method: 'POST',
                body: JSON.stringify({
                    nome: document.getElementById('cliNome').value,
                    telefone: document.getElementById('cliTelefone').value,
                    perfil: document.getElementById('cliPerfil').value,
                    email: document.getElementById('cliEmail').value,
                    endereco: document.getElementById('cliEndereco').value,
                    observacoes: document.getElementById('cliObs').value
                })
            });
            showToast('Cliente cadastrado com sucesso!');
            document.getElementById('formAdminCliente').reset();
            await carregarClientesAdmin();
        } catch (e) {
            showToast(e.message, true);
        }
    }

    async function verHistoricoClienteAdmin(clienteId, nome) {
        const hist = await api(`/api/admin/clientes/${clienteId}/historico`);
        const box = document.getElementById('boxHistoricoClienteAdmin');
        box.classList.remove('hidden');
        box.innerHTML = `<h4>Histórico de Atendimentos – ${escapeHtml(nome)} (${hist.length})</h4>` + (
            hist.length === 0
                ? '<div class="empty-state">Nenhuma visita registrada ainda.</div>'
                : hist.map(a => `<div class="appointment-item st-${a.statusCodigo}" style="padding:9px">
                    <strong>${formatarDataBr(a.data)} ${a.horaInicio}</strong> • ${escapeHtml(a.tipoServicoDescricao)}
                    <span class="badge status-${a.statusCodigo}">${escapeHtml(a.statusDescricao)}</span>
                  </div>`).join('')
        );
    }

    async function carregarConversasAdmin() {
        const conversas = await api('/api/admin/conversas');
        const box = document.getElementById('listaAdminConversas');
        box.innerHTML = conversas.length === 0
            ? '<div class="empty-state">Nenhuma conversa iniciada no chat.</div>'
            : conversas.map(c => `
                <div class="appointment-item" style="cursor:pointer" onclick="App.selecionarConversaAdmin(${c.id})">
                    <div class="appointment-top">
                        <strong>#${c.id} • ${escapeHtml(c.nomeCliente)}</strong>
                        <span class="badge status-${c.statusCodigo}">${escapeHtml(c.statusDescricao)}</span>
                    </div>
                    <div class="muted" style="font-size:0.82rem">Tel: ${escapeHtml(c.telefoneCliente || 'Não informado')} • ${c.mensagens.length} mensagens</div>
                </div>
            `).join('');

        if (conversaAdminSelecionada) {
            const atual = conversas.find(c => c.id === conversaAdminSelecionada);
            if (atual) renderizarDetalheConversaAdmin(atual);
        }
    }

    async function selecionarConversaAdmin(id) {
        conversaAdminSelecionada = id;
        const conversas = await api('/api/admin/conversas');
        const c = conversas.find(x => x.id === id);
        if (c) renderizarDetalheConversaAdmin(c);
    }

    function renderizarDetalheConversaAdmin(c) {
        const box = document.getElementById('boxAdminDetalheConversa');
        box.className = '';
        box.innerHTML = `
            <div style="margin-bottom:10px">
                <strong>Cliente: ${escapeHtml(c.nomeCliente)}</strong>
                <span class="badge status-${c.statusCodigo}">${escapeHtml(c.statusDescricao)}</span>
            </div>
            <div class="chat-messages" style="max-height:300px;background:#f8fafc;padding:10px;border-radius:8px;margin-bottom:12px">
                ${c.mensagens.map(m => `
                    <div class="msg ${m.remetente === 'CLIENTE' ? 'msg-bot' : 'msg-owner'}">
                        <div class="msg-meta">${escapeHtml(m.remetente)}</div>
                        <div class="msg-bubble">${escapeHtml(m.conteudo)}</div>
                    </div>
                `).join('')}
            </div>
            <form onsubmit="App.responderChatAdmin(event, ${c.id})">
                <div class="form-group">
                    <label>Responder como Proprietário (Carlos):</label>
                    <textarea id="inputRespostaOwnerChat" rows="2" required placeholder="Digite sua orientação técnica ou confirmação para o cliente..."></textarea>
                </div>
                <div class="inline-form">
                    <button type="submit" class="btn btn-primary">Enviar Resposta Humana</button>
                </div>
            </form>
        `;
    }

    async function responderChatAdmin(ev, conversaId) {
        ev.preventDefault();
        const resposta = document.getElementById('inputRespostaOwnerChat').value;
        await api(`/api/admin/conversas/${conversaId}/responder`, {
            method: 'POST',
            body: JSON.stringify({ resposta, encerrar: false })
        });
        showToast('Resposta enviada ao cliente!');
        await carregarConversasAdmin();
        await carregarDashboardEAgenda();
    }

    async function carregarNotificacoesAdmin() {
        const lista = await api('/api/admin/notificacoes');
        const box = document.getElementById('listaAdminNotificacoes');
        box.innerHTML = lista.length === 0
            ? '<div class="empty-state">Nenhuma notificação registrada.</div>'
            : lista.map(n => `
                <div class="appointment-item" style="padding:10px 14px;opacity:${n.lida ? '0.78' : '1'}">
                    <div class="appointment-top">
                        <strong>[${escapeHtml(n.destinatarioPapel)}] ${escapeHtml(n.titulo)}</strong>
                        <span class="badge status-CONFIRMADO">${escapeHtml(n.canalEnvio)}</span>
                    </div>
                    <div style="font-size:0.88rem">${escapeHtml(n.mensagem)}</div>
                </div>
            `).join('');
    }

    function bindEventos() {
        document.querySelectorAll('.mode-btn').forEach(btn => {
            btn.addEventListener('click', () => switchView(btn.dataset.view));
        });

        document.getElementById('agData').addEventListener('change', carregarHorariosLivresCliente);
        document.getElementById('formNovoAgendamento').addEventListener('submit', submeterNovoAgendamento);
        document.getElementById('btnBuscarProtocolo').addEventListener('click', consultarProtocoloCliente);
        document.getElementById('btnBuscarTelefone').addEventListener('click', consultarTelefoneCliente);
        document.getElementById('remNovaData').addEventListener('change', atualizarHorariosModalRemarcar);
        document.getElementById('formRemarcarCliente').addEventListener('submit', confirmarRemarcacaoCliente);

        document.getElementById('formEnviarChatCliente').addEventListener('submit', async ev => {
            ev.preventDefault();
            const inp = document.getElementById('chatTextoMensagem');
            const txt = inp.value;
            inp.value = '';
            await enviarMensagemChat(txt, false);
        });

        document.querySelectorAll('.chip-btn').forEach(chip => {
            chip.addEventListener('click', () => {
                enviarMensagemChat(chip.dataset.prompt, chip.dataset.human === 'true');
            });
        });

        document.getElementById('btnNovaConversaChat').addEventListener('click', () => {
            chatConversaId = null;
            document.getElementById('chatMessagesBox').innerHTML = `
                <div class="msg msg-bot">
                    <div class="msg-meta">Assistente Power Plus</div>
                    <div class="msg-bubble">Nova conversa iniciada! Como posso ajudar com sua instalação ou manutenção elétrica hoje?</div>
                </div>
            `;
        });

        document.getElementById('formOwnerLogin').addEventListener('submit', loginProprietario);
        document.getElementById('btnOwnerLogout').addEventListener('click', async () => {
            await api('/api/admin/auth/logout', { method: 'POST' }).catch(() => {});
            ownerToken = null;
            localStorage.removeItem('pp_owner_token');
            atualizarEstadoLoginOwner();
        });

        document.querySelectorAll('.subtab-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('.subtab-btn').forEach(b => b.classList.remove('active'));
                document.querySelectorAll('.owner-tab').forEach(t => t.classList.remove('active'));
                btn.classList.add('active');
                document.getElementById(btn.dataset.subtab).classList.add('active');
            });
        });

        document.getElementById('adminDataAgenda').addEventListener('change', carregarDashboardEAgenda);
        document.getElementById('btnModoDia').addEventListener('click', () => {
            modoVisaoAgenda = 'DIA';
            document.getElementById('btnModoDia').className = 'btn btn-sm btn-primary';
            document.getElementById('btnModoSemana').className = 'btn btn-sm btn-outline';
            carregarDashboardEAgenda();
        });
        document.getElementById('btnModoSemana').addEventListener('click', () => {
            modoVisaoAgenda = 'SEMANA';
            document.getElementById('btnModoSemana').className = 'btn btn-sm btn-primary';
            document.getElementById('btnModoDia').className = 'btn btn-sm btn-outline';
            carregarDashboardEAgenda();
        });

        document.getElementById('formNovoBloqueio').addEventListener('submit', ev => criarBloqueioAdmin(ev, false));
        document.getElementById('formAdminCliente').addEventListener('submit', cadastrarClienteAdmin);
        document.getElementById('btnAdminBuscarCliente').addEventListener('click', carregarClientesAdmin);
        document.getElementById('btnMarcarNotifLidas').addEventListener('click', async () => {
            await api('/api/admin/notificacoes/marcar-lidas', { method: 'POST' });
            await carregarNotificacoesAdmin();
            await carregarDashboardEAgenda();
        });
    }

    document.addEventListener('DOMContentLoaded', async () => {
        bindEventos();
        await carregarCatalogoInicial();
    });

    return {
        switchView,
        abrirModalRemarcar,
        fecharModalRemarcar,
        cancelarPeloCliente,
        alterarStatusAdmin,
        enviarLembreteAdmin,
        salvarDiaDisponibilidade,
        removerBloqueioAdmin,
        verHistoricoClienteAdmin,
        selecionarConversaAdmin,
        responderChatAdmin
    };
})();

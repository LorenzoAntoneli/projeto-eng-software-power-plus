document.addEventListener('DOMContentLoaded', async () => {
    if (!(await PP.initBackoffice('agenda'))) return;

    const inputData = document.getElementById('adminDataRef');
    const container = document.getElementById('agendaContainer');
    const filaAprovacaoMount = document.getElementById('filaAprovacaoMount');
    const filtroBar = document.getElementById('filtroStatusBar');
    const btnGeral = document.getElementById('btnModoGeral');
    const btnDia = document.getElementById('btnModoDia');
    const btnSemana = document.getElementById('btnModoSemana');
    let modo = 'GERAL';
    let filtroStatus = 'TODOS';

    inputData.value = PP.hojeIso();

    function definirModo(novoModo) {
        modo = novoModo;
        btnGeral?.classList.toggle('active', modo === 'GERAL');
        btnDia?.classList.toggle('active', modo === 'DIA');
        btnSemana?.classList.toggle('active', modo === 'SEMANA');
    }

    function definirFiltroStatus(novoFiltro) {
        filtroStatus = novoFiltro;
        filtroBar?.querySelectorAll('[data-filtro]').forEach(c => {
            c.classList.toggle('active', c.dataset.filtro === filtroStatus);
        });
    }

    function formatarTelefoneBr(valor) {
        const d = String(valor || '').replace(/\D/g, '');
        if (d.length === 11) return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
        if (d.length === 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
        return valor || '';
    }

    function deslocarDataIso(isoAtual, dias) {
        const d = new Date((isoAtual || PP.hojeIso()) + 'T12:00:00');
        d.setDate(d.getDate() + dias);
        return d.toISOString().slice(0, 10);
    }

    function linkWhatsApp(telefone, protocolo) {
        const limpo = String(telefone || '').replace(/\D/g, '');
        if (limpo.length < 10) return '';
        const texto = encodeURIComponent(`Olá! Aqui é o Leonardo da Power Plus referente ao protocolo ${protocolo}.`);
        return `<a class="phone-quick-link" href="https://wa.me/55${limpo}?text=${texto}" target="_blank" rel="noopener">Abrir WhatsApp</a>`;
    }

    function renderBotoesAcao(a) {
        if (a.statusCodigo === 'AGUARDANDO_APROVACAO') {
            return `
                <button type="button" class="btn btn-approve btn-sm" data-aprovar="${a.id}" data-prot="${PP.esc(a.protocolo)}" data-data="${a.data}">Aprovar visita</button>
                <button type="button" class="btn btn-danger btn-sm" data-recusar="${a.id}" data-prot="${PP.esc(a.protocolo)}">Recusar horário</button>
            `;
        }
        if (a.statusCodigo === 'CONFIRMADO') {
            return `
                <button type="button" class="btn btn-primary btn-sm" data-status="${a.id}" data-alvo="EM_ATENDIMENTO">Iniciar execução</button>
                <button type="button" class="btn btn-secondary btn-sm" data-status="${a.id}" data-alvo="CONCLUIDO">Concluir ordem</button>
                <button type="button" class="btn btn-secondary btn-sm" data-lembrete="${a.id}">Disparar lembrete</button>
                <button type="button" class="btn btn-danger btn-sm" data-status="${a.id}" data-alvo="CANCELADO">Cancelar</button>
            `;
        }
        if (a.statusCodigo === 'EM_ATENDIMENTO') {
            return `
                <button type="button" class="btn btn-primary btn-sm" data-status="${a.id}" data-alvo="CONCLUIDO">Concluir ordem</button>
                <button type="button" class="btn btn-danger btn-sm" data-status="${a.id}" data-alvo="CANCELADO">Cancelar</button>
            `;
        }
        return '';
    }

    function renderCardDia(a, exibirDataNoTopo = false) {
        const botoes = renderBotoesAcao(a);
        const prefixoData = exibirDataNoTopo ? `${PP.formatarDataBr(a.data)} · ` : '';

        return `
            <div class="order-item os-${a.statusCodigo}">
                <div class="order-head">
                    <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
                        <strong class="mono" style="font-size:0.95rem;color:var(--navy-900)">${prefixoData}${a.horaInicio} às ${a.horaFim}</strong>
                        <span style="font-weight:700">${PP.esc(a.clienteNome)}</span>
                        <span class="hint">${PP.esc(a.clientePerfil)}</span>
                    </div>
                    <div style="display:flex;align-items:center;gap:10px">
                        <span class="hint code-ref">${PP.esc(a.protocolo)}</span>
                        <span class="status-badge st-${a.statusCodigo}">${PP.esc(a.statusDescricao)}</span>
                    </div>
                </div>
                <div class="order-meta">
                    <div><strong>Serviço:</strong> ${PP.esc(a.tipoServicoDescricao)}</div>
                    <div><strong>Contato:</strong> <span class="mono">${PP.esc(formatarTelefoneBr(a.clienteTelefone))}</span>${linkWhatsApp(a.clienteTelefone, a.protocolo)}</div>
                    <div style="grid-column:1/-1"><strong>Endereço:</strong> ${PP.esc(a.enderecoAtendimento)}</div>
                    ${a.descricaoServico ? `<div style="grid-column:1/-1"><strong>Relato do cliente:</strong> ${PP.esc(a.descricaoServico)}</div>` : ''}
                    ${a.observacaoTecnica ? `<div style="grid-column:1/-1"><strong>Parecer técnico:</strong> ${PP.esc(a.observacaoTecnica)}</div>` : ''}
                </div>
                ${botoes ? `<div class="order-foot">${botoes}</div>` : ''}
            </div>
        `;
    }

    function renderFilaAprovacao(pendentes) {
        if (!filaAprovacaoMount) return;
        if (!pendentes || pendentes.length === 0) {
            filaAprovacaoMount.innerHTML = '';
            return;
        }
        filaAprovacaoMount.innerHTML = `
            <div class="approval-queue-box">
                <div class="approval-queue-head">
                    <h2>Solicitações pendentes de aprovação (${pendentes.length})</h2>
                    <span class="hint">Aprove para confirmar a visita ao cliente ou recuse para liberar imediatamente o horário.</span>
                </div>
                ${pendentes.map(p => renderCardDia(p, true)).join('')}
            </div>
        `;
        bindAcoesContainer(filaAprovacaoMount);
    }

    async function atualizarAgenda() {
        const data = inputData.value || PP.hojeIso();
        const dash = await PP.api(`/api/admin/dashboard?data=${data}`);
        const todos = dash.todosAgendamentos || dash.atendimentosHoje || [];

        const kpiPend = document.getElementById('kpiPendentesAprovacao');
        if (kpiPend) {
            kpiPend.textContent = dash.pendentesAprovacaoTotal ?? 0;
            kpiPend.style.color = (dash.pendentesAprovacaoTotal > 0) ? 'var(--accent)' : 'var(--navy-900)';
        }

        const kpiTitTotal = document.getElementById('kpiTituloTotal');
        const kpiTitConc = document.getElementById('kpiTituloConcluidas');
        if (modo === 'GERAL') {
            if (kpiTitTotal) kpiTitTotal.textContent = 'Total de ordens';
            if (kpiTitConc) kpiTitConc.textContent = 'Concluídas';
            document.getElementById('kpiDiaTotal').textContent = todos.length;
            document.getElementById('kpiDiaPendentes').textContent = todos.filter(
                a => a.statusCodigo === 'CONFIRMADO' || a.statusCodigo === 'EM_ATENDIMENTO'
            ).length;
            document.getElementById('kpiDiaConcluidas').textContent = todos.filter(
                a => a.statusCodigo === 'CONCLUIDO'
            ).length;
        } else {
            if (kpiTitTotal) kpiTitTotal.textContent = 'Ordens na data';
            if (kpiTitConc) kpiTitConc.textContent = 'Concluídas na data';
            document.getElementById('kpiDiaTotal').textContent = dash.totalHoje;
            document.getElementById('kpiDiaPendentes').textContent = dash.confirmadosHoje + dash.emAtendimentoHoje;
            document.getElementById('kpiDiaConcluidas').textContent = dash.concluidosHoje;
        }

        renderFilaAprovacao(dash.pendentesAprovacao || []);

        if (modo === 'GERAL' || modo === 'DIA') {
            if (filtroBar) filtroBar.classList.remove('hidden');
            const baseLista = modo === 'GERAL' ? todos : dash.atendimentosHoje;
            const listaFiltrada = filtroStatus === 'TODOS'
                ? baseLista
                : baseLista.filter(a => a.statusCodigo === filtroStatus);

            if (!listaFiltrada.length) {
                container.innerHTML = modo === 'GERAL'
                    ? '<div class="empty-placeholder">Nenhuma ordem encontrada neste filtro de status.</div>'
                    : `<div class="empty-placeholder">Nenhuma ordem programada para ${PP.formatarDataBr(data)} neste filtro. Clique em <strong>Lista geral</strong> para visualizar todas as datas ou alterne para <strong>Semana</strong>.</div>`;
                return;
            }
            container.innerHTML = listaFiltrada.map(a => renderCardDia(a, true)).join('');
            bindAcoesContainer(container);
        } else {
            if (filtroBar) filtroBar.classList.add('hidden');
            const sem = await PP.api(`/api/admin/agenda/semana?dataReferencia=${data}`);
            const porDia = {};
            sem.agendamentos.forEach(a => {
                porDia[a.data] = porDia[a.data] || [];
                porDia[a.data].push(a);
            });

            let htmlCols = '';
            const cursor = new Date(sem.inicioSemana + 'T12:00:00');
            for (let i = 0; i < 6; i++) {
                const iso = cursor.toISOString().slice(0, 10);
                const itens = porDia[iso] || [];
                htmlCols += `
                    <div class="week-column">
                        <div class="week-day-header">${PP.formatarDataExtenso(iso).split(' de ')[0]} · ${iso.slice(8,10)}/${iso.slice(5,7)}</div>
                        ${itens.length === 0
                            ? '<span class="hint">Sem ordens</span>'
                            : itens.map(a => `
                                <div class="week-slot-card" style="cursor:pointer" data-jump-date="${a.data}">
                                    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;gap:4px">
                                        <strong class="mono">${a.horaInicio}</strong>
                                        <span class="status-badge st-${a.statusCodigo}" style="font-size:0.62rem;padding:1px 5px">${PP.esc(a.statusDescricao)}</span>
                                    </div>
                                    <div style="font-weight:700;margin-bottom:2px">${PP.esc(a.clienteNome)}</div>
                                    <div class="hint" style="font-size:0.73rem">${PP.esc(a.tipoServicoDescricao)}</div>
                                </div>
                              `).join('')}
                    </div>
                `;
                cursor.setDate(cursor.getDate() + 1);
            }
            container.innerHTML = `<div class="week-board">${htmlCols}</div>`;
            container.querySelectorAll('[data-jump-date]').forEach(card => {
                card.addEventListener('click', () => {
                    inputData.value = card.dataset.jumpDate;
                    definirModo('DIA');
                    atualizarAgenda();
                });
            });
        }
    }

    function bindAcoesContainer(rootEl) {
        rootEl.querySelectorAll('[data-aprovar]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.dataset.aprovar;
                const prot = btn.dataset.prot;
                const dataVisita = btn.dataset.data;
                btn.disabled = true;
                try {
                    await PP.api(`/api/admin/agendamentos/${id}/aprovar`, {
                        method: 'POST',
                        body: JSON.stringify({ observacao: 'Visita aprovada pelo Leonardo.' })
                    });
                    if (modo === 'DIA' && dataVisita && inputData.value !== dataVisita) {
                        inputData.value = dataVisita;
                    }
                    if (filtroStatus === 'AGUARDANDO_APROVACAO') {
                        definirFiltroStatus('TODOS');
                    }
                    PP.toast(`Ordem ${prot} aprovada e confirmada na agenda.`);
                    await atualizarAgenda();
                    await PP.refreshSidebarCounters();
                } catch (err) {
                    btn.disabled = false;
                    PP.toast(err.message, true);
                }
            });
        });

        rootEl.querySelectorAll('[data-recusar]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.dataset.recusar;
                const prot = btn.dataset.prot;
                const motivo = await PP.openDialog({
                    title: `Recusar solicitação ${prot}`,
                    description: 'O horário voltará a ficar livre na agenda pública e o solicitante visualizará a justificativa informada.',
                    inputLabel: 'Justificativa da recusa',
                    defaultValue: 'Indisponibilidade técnica neste horário — por favor selecione outra janela disponível.',
                    confirmLabel: 'Confirmar recusa'
                });
                if (motivo === null) return;
                try {
                    await PP.api(`/api/admin/agendamentos/${id}/recusar`, {
                        method: 'POST',
                        body: JSON.stringify({ observacao: motivo })
                    });
                    PP.toast(`Solicitação ${prot} recusada e horário liberado.`);
                    await atualizarAgenda();
                    await PP.refreshSidebarCounters();
                } catch (err) {
                    PP.toast(err.message, true);
                }
            });
        });

        rootEl.querySelectorAll('[data-status]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.dataset.status;
                const novoStatus = btn.dataset.alvo;
                let observacao = '';

                if (novoStatus === 'CONCLUIDO') {
                    const res = await PP.openDialog({
                        title: 'Concluir ordem de serviço',
                        description: 'Registre opcionalmente o parecer técnico do atendimento executado.',
                        inputLabel: 'Parecer / nota técnica',
                        defaultValue: 'Serviço executado e testado conforme especificações.',
                        confirmLabel: 'Concluir ordem'
                    });
                    if (res === null) return;
                    observacao = res;
                } else if (novoStatus === 'CANCELADO') {
                    const res = await PP.openDialog({
                        title: 'Cancelar ordem de serviço',
                        description: 'O horário voltará a ficar disponível na agenda pública.',
                        inputLabel: 'Motivo do cancelamento',
                        defaultValue: 'Readequação de roteiro técnico',
                        confirmLabel: 'Confirmar cancelamento'
                    });
                    if (res === null) return;
                    observacao = res;
                }

                try {
                    await PP.api(`/api/admin/agendamentos/${id}/status`, {
                        method: 'PATCH',
                        body: JSON.stringify({ novoStatus, observacao })
                    });
                    PP.toast('Situação da ordem atualizada.');
                    await atualizarAgenda();
                    await PP.refreshSidebarCounters();
                } catch (err) {
                    PP.toast(err.message, true);
                }
            });
        });

        rootEl.querySelectorAll('[data-lembrete]').forEach(btn => {
            btn.addEventListener('click', async () => {
                try {
                    const res = await PP.api(`/api/admin/agendamentos/${btn.dataset.lembrete}/lembrete`, { method: 'POST' });
                    PP.toast(res.mensagem);
                    await PP.refreshSidebarCounters();
                } catch (err) {
                    PP.toast(err.message, true);
                }
            });
        });
    }

    inputData.addEventListener('change', () => {
        if (modo === 'GERAL') definirModo('DIA');
        atualizarAgenda();
    });
    document.getElementById('btnDiaAnterior')?.addEventListener('click', () => {
        inputData.value = deslocarDataIso(inputData.value, -1);
        if (modo === 'GERAL') definirModo('DIA');
        atualizarAgenda();
    });
    document.getElementById('btnProximoDia')?.addEventListener('click', () => {
        inputData.value = deslocarDataIso(inputData.value, 1);
        if (modo === 'GERAL') definirModo('DIA');
        atualizarAgenda();
    });
    document.getElementById('btnHoje').addEventListener('click', () => {
        inputData.value = PP.hojeIso();
        definirModo('DIA');
        atualizarAgenda();
    });
    btnGeral?.addEventListener('click', () => {
        definirModo('GERAL');
        atualizarAgenda();
    });
    btnDia.addEventListener('click', () => {
        definirModo('DIA');
        atualizarAgenda();
    });
    btnSemana.addEventListener('click', () => {
        definirModo('SEMANA');
        atualizarAgenda();
    });
    filtroBar?.querySelectorAll('[data-filtro]').forEach(chip => {
        chip.addEventListener('click', () => {
            definirFiltroStatus(chip.dataset.filtro);
            atualizarAgenda();
        });
    });

    await atualizarAgenda();
});

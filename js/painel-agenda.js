document.addEventListener('DOMContentLoaded', async () => {
    if (!(await PP.initBackoffice('agenda'))) return;

    const inputData = document.getElementById('adminDataRef');
    const container = document.getElementById('agendaContainer');
    const filaAprovacaoMount = document.getElementById('filaAprovacaoMount');
    const filtroBar = document.getElementById('filtroStatusBar');
    const btnDia = document.getElementById('btnModoDia');
    const btnSemana = document.getElementById('btnModoSemana');
    let modo = 'DIA';
    let filtroStatus = 'TODOS';

    inputData.value = PP.hojeIso();

    function deslocarDataIso(isoAtual, dias) {
        const d = new Date((isoAtual || PP.hojeIso()) + 'T12:00:00');
        d.setDate(d.getDate() + dias);
        return d.toISOString().slice(0, 10);
    }

    function linkWhatsApp(telefone, protocolo) {
        const limpo = String(telefone || '').replace(/\D/g, '');
        if (limpo.length < 10) return '';
        const texto = encodeURIComponent(`Olá! Aqui é o Leonardo da Power Plus sobre o atendimento ${protocolo}.`);
        return `<a class="phone-quick-link" href="https://wa.me/55${limpo}?text=${texto}" target="_blank" rel="noopener">WhatsApp ↗</a>`;
    }

    function renderBotoesAcao(a) {
        if (a.statusCodigo === 'AGUARDANDO_APROVACAO') {
            return `
                <button type="button" class="btn btn-approve btn-sm" data-aprovar="${a.id}" data-prot="${PP.esc(a.protocolo)}">✓ Aprovar visita</button>
                <button type="button" class="btn btn-danger btn-sm" data-recusar="${a.id}" data-prot="${PP.esc(a.protocolo)}">✕ Recusar</button>
            `;
        }
        if (a.statusCodigo === 'CONFIRMADO') {
            return `
                <button type="button" class="btn btn-primary btn-sm" data-status="${a.id}" data-alvo="EM_ATENDIMENTO">Iniciar atendimento</button>
                <button type="button" class="btn btn-secondary btn-sm" data-status="${a.id}" data-alvo="CONCLUIDO">Concluir serviço</button>
                <button type="button" class="btn btn-secondary btn-sm" data-lembrete="${a.id}">Enviar lembrete</button>
                <button type="button" class="btn btn-danger btn-sm" data-status="${a.id}" data-alvo="CANCELADO">Cancelar</button>
            `;
        }
        if (a.statusCodigo === 'EM_ATENDIMENTO') {
            return `
                <button type="button" class="btn btn-primary btn-sm" data-status="${a.id}" data-alvo="CONCLUIDO">Concluir serviço</button>
                <button type="button" class="btn btn-danger btn-sm" data-status="${a.id}" data-alvo="CANCELADO">Cancelar</button>
            `;
        }
        return '';
    }

    function renderCardDia(a, exibirDataNoTopo = false) {
        const botoes = renderBotoesAcao(a);
        const prefixoData = exibirDataNoTopo ? `${PP.formatarDataBr(a.data)} · ` : '';

        return `
            <div class="order-item">
                <div class="order-head">
                    <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
                        <strong class="mono" style="font-size:0.95rem">${prefixoData}${a.horaInicio}–${a.horaFim}</strong>
                        <span style="font-weight:600">${PP.esc(a.clienteNome)}</span>
                        <span class="hint">${PP.esc(a.clientePerfil)}</span>
                    </div>
                    <div style="display:flex;align-items:center;gap:10px">
                        <span class="hint mono">${PP.esc(a.protocolo)}</span>
                        <span class="status-badge st-${a.statusCodigo}">${PP.esc(a.statusDescricao)}</span>
                    </div>
                </div>
                <div class="order-meta">
                    <div><strong>Serviço:</strong> ${PP.esc(a.tipoServicoDescricao)}</div>
                    <div><strong>Telefone:</strong> <span class="mono">${PP.esc(a.clienteTelefone)}</span>${linkWhatsApp(a.clienteTelefone, a.protocolo)}</div>
                    <div style="grid-column:1/-1"><strong>Local:</strong> ${PP.esc(a.enderecoAtendimento)}</div>
                    ${a.descricaoServico ? `<div style="grid-column:1/-1"><strong>Descrição do cliente:</strong> ${PP.esc(a.descricaoServico)}</div>` : ''}
                    ${a.observacaoTecnica ? `<div style="grid-column:1/-1"><strong>Observação técnica:</strong> ${PP.esc(a.observacaoTecnica)}</div>` : ''}
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
                    <h2>Solicitações aguardando sua aprovação (${pendentes.length})</h2>
                    <span class="hint">Aprove para confirmar ao cliente ou recuse para liberar o horário na agenda.</span>
                </div>
                ${pendentes.map(p => renderCardDia(p, true)).join('')}
            </div>
        `;
        bindAcoesContainer(filaAprovacaoMount);
    }

    async function atualizarAgenda() {
        const data = inputData.value || PP.hojeIso();
        const dash = await PP.api(`/api/admin/dashboard?data=${data}`);

        const kpiPend = document.getElementById('kpiPendentesAprovacao');
        if (kpiPend) {
            kpiPend.textContent = dash.pendentesAprovacaoTotal ?? 0;
            kpiPend.style.color = (dash.pendentesAprovacaoTotal > 0) ? 'var(--amber)' : 'var(--ink)';
        }
        document.getElementById('kpiDiaTotal').textContent = dash.totalHoje;
        document.getElementById('kpiDiaPendentes').textContent = dash.confirmadosHoje + dash.emAtendimentoHoje;
        document.getElementById('kpiDiaConcluidas').textContent = dash.concluidosHoje;

        renderFilaAprovacao(dash.pendentesAprovacao || []);

        if (modo === 'DIA') {
            if (filtroBar) filtroBar.classList.remove('hidden');
            const listaFiltrada = filtroStatus === 'TODOS'
                ? dash.atendimentosHoje
                : dash.atendimentosHoje.filter(a => a.statusCodigo === filtroStatus);

            if (!listaFiltrada.length) {
                container.innerHTML = `<div class="empty-placeholder">Nenhuma visita encontrada para ${PP.formatarDataBr(data)} neste filtro. Use os botões <strong>◀ / ▶</strong> ou altere para a visão <strong>Semana</strong>.</div>`;
                return;
            }
            container.innerHTML = listaFiltrada.map(a => renderCardDia(a, false)).join('');
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
                            ? '<span class="hint">Livre</span>'
                            : itens.map(a => `
                                <div class="week-slot-card" style="cursor:pointer" data-jump-date="${a.data}">
                                    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;gap:4px">
                                        <strong class="mono">${a.horaInicio}</strong>
                                        <span class="status-badge st-${a.statusCodigo}" style="font-size:0.66rem;padding:1px 6px">${PP.esc(a.statusDescricao)}</span>
                                    </div>
                                    <div style="font-weight:600;margin-bottom:2px">${PP.esc(a.clienteNome)}</div>
                                    <div class="hint" style="font-size:0.74rem">${PP.esc(a.tipoServicoDescricao)}</div>
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
                    modo = 'DIA';
                    btnDia.classList.add('active');
                    btnSemana.classList.remove('active');
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
                btn.disabled = true;
                try {
                    await PP.api(`/api/admin/agendamentos/${id}/aprovar`, {
                        method: 'POST',
                        body: JSON.stringify({ observacao: 'Visita aprovada pelo Leonardo.' })
                    });
                    PP.toast(`Visita ${prot} aprovada e confirmada!`);
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
                    description: 'O horário voltará a ficar livre imediatamente na agenda pública e o cliente verá o motivo informado.',
                    inputLabel: 'Motivo da recusa',
                    defaultValue: 'Indisponibilidade técnica neste horário — por favor escolha outro horário livre.',
                    confirmLabel: 'Confirmar recusa'
                });
                if (motivo === null) return;
                try {
                    await PP.api(`/api/admin/agendamentos/${id}/recusar`, {
                        method: 'POST',
                        body: JSON.stringify({ observacao: motivo })
                    });
                    PP.toast(`Solicitação ${prot} recusada e vaga liberada.`);
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
                        title: 'Concluir atendimento',
                        description: 'Registre opcionalmente o parecer técnico do serviço executado.',
                        inputLabel: 'Parecer / nota técnica',
                        defaultValue: 'Serviço executado e testado conforme especificações.',
                        confirmLabel: 'Concluir visita'
                    });
                    if (res === null) return;
                    observacao = res;
                } else if (novoStatus === 'CANCELADO') {
                    const res = await PP.openDialog({
                        title: 'Cancelar atendimento',
                        description: 'O horário voltará a ficar livre na agenda pública.',
                        inputLabel: 'Motivo do cancelamento',
                        defaultValue: 'Ajuste operacional na agenda',
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
                    PP.toast('Status atualizado com sucesso.');
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

    inputData.addEventListener('change', atualizarAgenda);
    document.getElementById('btnDiaAnterior')?.addEventListener('click', () => {
        inputData.value = deslocarDataIso(inputData.value, -1);
        atualizarAgenda();
    });
    document.getElementById('btnProximoDia')?.addEventListener('click', () => {
        inputData.value = deslocarDataIso(inputData.value, 1);
        atualizarAgenda();
    });
    document.getElementById('btnHoje').addEventListener('click', () => {
        inputData.value = PP.hojeIso();
        atualizarAgenda();
    });
    btnDia.addEventListener('click', () => {
        modo = 'DIA';
        btnDia.classList.add('active');
        btnSemana.classList.remove('active');
        atualizarAgenda();
    });
    btnSemana.addEventListener('click', () => {
        modo = 'SEMANA';
        btnSemana.classList.add('active');
        btnDia.classList.remove('active');
        atualizarAgenda();
    });
    filtroBar?.querySelectorAll('[data-filtro]').forEach(chip => {
        chip.addEventListener('click', () => {
            filtroStatus = chip.dataset.filtro;
            filtroBar.querySelectorAll('[data-filtro]').forEach(c => c.classList.remove('active'));
            chip.classList.add('active');
            atualizarAgenda();
        });
    });

    const dashInicial = await PP.api(`/api/admin/dashboard?data=${inputData.value}`);
    if (dashInicial.totalHoje === 0 && dashInicial.totalSemana > 0) {
        modo = 'SEMANA';
        btnSemana.classList.add('active');
        btnDia.classList.remove('active');
    }
    await atualizarAgenda();
});

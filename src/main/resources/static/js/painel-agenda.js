document.addEventListener('DOMContentLoaded', async () => {
    if (!(await PP.initBackoffice('agenda'))) return;

    const inputData = document.getElementById('adminDataRef');
    const container = document.getElementById('agendaContainer');
    const btnDia = document.getElementById('btnModoDia');
    const btnSemana = document.getElementById('btnModoSemana');
    let modo = 'DIA';

    inputData.value = PP.hojeIso();

    function renderCardDia(a) {
        let botoes = '';
        if (a.statusCodigo === 'CONFIRMADO') {
            botoes = `
                <button type="button" class="btn btn-primary btn-sm" data-status="${a.id}" data-alvo="EM_ATENDIMENTO">Iniciar atendimento</button>
                <button type="button" class="btn btn-secondary btn-sm" data-status="${a.id}" data-alvo="CONCLUIDO">Concluir serviço</button>
                <button type="button" class="btn btn-secondary btn-sm" data-lembrete="${a.id}">Enviar lembrete</button>
                <button type="button" class="btn btn-danger btn-sm" data-status="${a.id}" data-alvo="CANCELADO">Cancelar</button>
            `;
        } else if (a.statusCodigo === 'EM_ATENDIMENTO') {
            botoes = `
                <button type="button" class="btn btn-primary btn-sm" data-status="${a.id}" data-alvo="CONCLUIDO">Concluir serviço</button>
                <button type="button" class="btn btn-danger btn-sm" data-status="${a.id}" data-alvo="CANCELADO">Cancelar</button>
            `;
        }

        return `
            <div class="order-item">
                <div class="order-head">
                    <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
                        <strong class="mono" style="font-size:0.95rem">${a.horaInicio}–${a.horaFim}</strong>
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
                    <div><strong>Telefone:</strong> <span class="mono">${PP.esc(a.clienteTelefone)}</span></div>
                    <div style="grid-column:1/-1"><strong>Local:</strong> ${PP.esc(a.enderecoAtendimento)}</div>
                    ${a.descricaoServico ? `<div style="grid-column:1/-1"><strong>Descrição do cliente:</strong> ${PP.esc(a.descricaoServico)}</div>` : ''}
                    ${a.observacaoTecnica ? `<div style="grid-column:1/-1"><strong>Parecer técnico:</strong> ${PP.esc(a.observacaoTecnica)}</div>` : ''}
                </div>
                ${botoes ? `<div class="order-foot">${botoes}</div>` : ''}
            </div>
        `;
    }

    async function atualizarAgenda() {
        const data = inputData.value || PP.hojeIso();
        const dash = await PP.api(`/api/admin/dashboard?data=${data}`);

        document.getElementById('kpiDiaTotal').textContent = dash.totalHoje;
        document.getElementById('kpiDiaPendentes').textContent = dash.confirmadosHoje + dash.emAtendimentoHoje;
        document.getElementById('kpiDiaConcluidas').textContent = dash.concluidosHoje;
        document.getElementById('kpiSemanaTotal').textContent = dash.totalSemana;

        if (modo === 'DIA') {
            if (!dash.atendimentosHoje.length) {
                container.innerHTML = `<div class="empty-placeholder">Nenhuma visita agendada para ${PP.formatarDataBr(data)}. Altere para a visão <strong>Semana</strong> acima para ver todos os agendamentos da semana.</div>`;
                return;
            }
            container.innerHTML = dash.atendimentosHoje.map(renderCardDia).join('');
            bindBotoesAgenda();
        } else {
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
                                    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px">
                                        <strong class="mono">${a.horaInicio}</strong>
                                        <span class="status-badge st-${a.statusCodigo}" style="font-size:0.68rem;padding:1px 6px">${PP.esc(a.statusDescricao)}</span>
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

    function bindBotoesAgenda() {
        container.querySelectorAll('[data-status]').forEach(btn => {
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

        container.querySelectorAll('[data-lembrete]').forEach(btn => {
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

    // Inicia na visão Semana caso hoje não tenha visitas para mostrar imediatamente os agendamentos ativos
    const dashInicial = await PP.api(`/api/admin/dashboard?data=${inputData.value}`);
    if (dashInicial.totalHoje === 0 && dashInicial.totalSemana > 0) {
        modo = 'SEMANA';
        btnSemana.classList.add('active');
        btnDia.classList.remove('active');
    }
    await atualizarAgenda();
});


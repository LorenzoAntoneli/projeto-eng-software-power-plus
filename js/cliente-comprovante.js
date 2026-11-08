document.addEventListener('DOMContentLoaded', async () => {
    const params = new URLSearchParams(window.location.search);
    const protocolo = params.get('protocolo');
    const mount = document.getElementById('ticketMount');

    if (!protocolo) {
        mount.innerHTML = `
            <div class="ticket-body">
                <h2 style="font-size:1.1rem;margin-bottom:8px">Nenhum protocolo informado</h2>
                <p class="hint" style="margin-bottom:16px">Consulte um agendamento existente ou envie uma nova solicitação de visita.</p>
                <a href="acompanhar.html" class="btn btn-primary">Consultar agendamento</a>
            </div>
        `;
        return;
    }

    function montarAvisoStatus(a) {
        if (a.statusCodigo === 'AGUARDANDO_APROVACAO') {
            return {
                titulo: 'Solicitação enviada — aguardando aprovação',
                aviso: `<div class="ticket-notice pending">
                    <strong>Horário pré-reservado para você!</strong> Recebemos seu pedido e bloqueamos provisoriamente este horário na agenda. Assim que o <strong>Leonardo</strong> aprovar sua visita, o status mudará para <strong>Confirmado</strong>. Guarde seu protocolo abaixo para acompanhar.
                </div>`
            };
        }
        if (a.statusCodigo === 'CONFIRMADO') {
            return {
                titulo: 'Visita técnica aprovada e confirmada',
                aviso: `<div class="ticket-notice confirmed">
                    <strong>Visita confirmada pelo Leonardo!</strong> O atendimento está programado para a data e janela abaixo.${a.observacaoTecnica ? ` Observação: <em>${PP.esc(a.observacaoTecnica)}</em>` : ''}
                </div>`
            };
        }
        if (a.statusCodigo === 'RECUSADO') {
            return {
                titulo: 'Solicitação não aprovada neste horário',
                aviso: `<div class="ticket-notice refused">
                    <strong>Não foi possível atender neste horário.</strong> ${a.observacaoTecnica ? `Motivo informado pelo Leonardo: <em>${PP.esc(a.observacaoTecnica)}</em>.` : ''} Você pode escolher outro horário livre clicando em <strong>Solicitar novo horário</strong> abaixo.
                </div>`
            };
        }
        if (a.statusCodigo === 'CANCELADO') {
            return {
                titulo: 'Agendamento cancelado',
                aviso: `<div class="ticket-notice refused">Esta visita foi cancelada e o horário foi liberado na agenda.</div>`
            };
        }
        return {
            titulo: 'Comprovante de atendimento elétrico',
            aviso: ''
        };
    }

    try {
        const res = await PP.api(`/api/publico/agendamentos/protocolo/${encodeURIComponent(protocolo)}`);
        const a = res.agendamento;
        const info = montarAvisoStatus(a);

        mount.innerHTML = `
            <div class="ticket-banner">
                <div>
                    <span class="status-badge st-${a.statusCodigo}" style="margin-bottom:6px">${PP.esc(a.statusDescricao)}</span>
                    <h1 style="font-size:1.28rem;font-weight:600;letter-spacing:-0.02em">${info.titulo}</h1>
                </div>
                <div style="text-align:right">
                    <div class="kv-label">Protocolo</div>
                    <div class="mono" style="font-size:1.05rem;font-weight:600">${PP.esc(a.protocolo)}</div>
                </div>
            </div>

            <div class="ticket-body">
                ${info.aviso}
                <div class="ticket-kv-grid">
                    <div>
                        <div class="kv-label">Data e janela de horário</div>
                        <div class="kv-val mono">${PP.formatarDataBr(a.data)} · ${a.horaInicio} às ${a.horaFim}</div>
                    </div>
                    <div>
                        <div class="kv-label">Tipo de serviço</div>
                        <div class="kv-val">${PP.esc(a.tipoServicoDescricao)}</div>
                    </div>
                    <div>
                        <div class="kv-label">Cliente / Imóvel</div>
                        <div class="kv-val">${PP.esc(a.clienteNome)} (${PP.esc(a.clientePerfil)})</div>
                    </div>
                    <div>
                        <div class="kv-label">Contato registrado</div>
                        <div class="kv-val mono">${PP.esc(a.clienteTelefone)}</div>
                    </div>
                    <div style="grid-column:1/-1">
                        <div class="kv-label">Local do atendimento</div>
                        <div class="kv-val">${PP.esc(a.enderecoAtendimento)}</div>
                    </div>
                    ${a.descricaoServico ? `
                        <div style="grid-column:1/-1">
                            <div class="kv-label">Observações informadas</div>
                            <div class="kv-val">${PP.esc(a.descricaoServico)}</div>
                        </div>
                    ` : ''}
                </div>
            </div>

            <div class="ticket-actions">
                <div style="display:flex;gap:8px;flex-wrap:wrap">
                    <button type="button" class="btn btn-secondary btn-sm" id="btnCopiarProt">Copiar protocolo</button>
                    <button type="button" class="btn btn-secondary btn-sm" onclick="window.print()">Imprimir comprovante</button>
                </div>
                <div style="display:flex;gap:8px;flex-wrap:wrap">
                    <a href="acompanhar.html?protocolo=${encodeURIComponent(a.protocolo)}" class="btn btn-secondary btn-sm">Acompanhar status / Remarcar</a>
                    <a href="index.html" class="btn btn-primary btn-sm">${a.statusCodigo === 'RECUSADO' ? 'Solicitar novo horário' : 'Nova solicitação'}</a>
                </div>
            </div>
        `;

        document.getElementById('btnCopiarProt').addEventListener('click', () => {
            navigator.clipboard?.writeText(a.protocolo);
            PP.toast(`Protocolo ${a.protocolo} copiado.`);
        });
    } catch (err) {
        mount.innerHTML = `
            <div class="ticket-body">
                <h2 style="font-size:1.1rem;margin-bottom:8px">Protocolo não encontrado</h2>
                <p class="hint" style="margin-bottom:16px">${PP.esc(err.message)}</p>
                <a href="acompanhar.html" class="btn btn-secondary">Voltar à consulta</a>
            </div>
        `;
    }
});

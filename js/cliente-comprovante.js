document.addEventListener('DOMContentLoaded', async () => {
    const params = new URLSearchParams(window.location.search);
    const protocolo = params.get('protocolo');
    const mount = document.getElementById('ticketMount');

    if (!protocolo) {
        mount.innerHTML = `
            <div class="ticket-body">
                <h2 style="font-size:1.1rem;margin-bottom:8px">Nenhum protocolo informado</h2>
                <p class="hint" style="margin-bottom:16px">Consulte um agendamento existente ou realize uma nova reserva.</p>
                <a href="acompanhar.html" class="btn btn-primary">Consultar agendamento</a>
            </div>
        `;
        return;
    }

    try {
        const res = await PP.api(`/api/publico/agendamentos/protocolo/${encodeURIComponent(protocolo)}`);
        const a = res.agendamento;

        mount.innerHTML = `
            <div class="ticket-banner">
                <div>
                    <span class="status-badge st-${a.statusCodigo}" style="margin-bottom:6px">${PP.esc(a.statusDescricao)}</span>
                    <h1 style="font-size:1.28rem;font-weight:600;letter-spacing:-0.02em">Ordem de agendamento registrada</h1>
                </div>
                <div style="text-align:right">
                    <div class="kv-label">Protocolo</div>
                    <div class="mono" style="font-size:1.05rem;font-weight:600">${PP.esc(a.protocolo)}</div>
                </div>
            </div>

            <div class="ticket-body">
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
                    <a href="acompanhar.html?protocolo=${encodeURIComponent(a.protocolo)}" class="btn btn-secondary btn-sm">Gerenciar / Remarcar</a>
                    <a href="index.html" class="btn btn-primary btn-sm">Novo agendamento</a>
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


document.addEventListener('DOMContentLoaded', async () => {
    const params = new URLSearchParams(window.location.search);
    const protocolo = params.get('protocolo');
    const mount = document.getElementById('ticketMount');

    if (!protocolo) {
        mount.innerHTML = `
            <div class="ticket-body">
                <h2 style="font-size:1.1rem;margin-bottom:8px;color:var(--navy-900)">Protocolo não informado</h2>
                <p class="hint" style="margin-bottom:16px">Informe um código de protocolo existente ou registre uma nova solicitação de visita.</p>
                <a href="acompanhar.html" class="btn btn-primary">Consultar protocolo</a>
            </div>
        `;
        return;
    }

    function montarAvisoStatus(a) {
        if (a.statusCodigo === 'AGUARDANDO_APROVACAO') {
            return {
                titulo: 'Solicitação registrada — aguardando aprovação',
                aviso: `<div class="ticket-notice pending">
                    <strong>Horário pré-reservado na agenda.</strong> Seu chamado foi recebido e a vaga encontra-se bloqueada provisoriamente. Assim que o responsável técnico <strong>Leonardo</strong> aprovar a visita, a situação mudará para <strong>Confirmado</strong>. Guarde o número do protocolo para consulta.
                </div>`
            };
        }
        if (a.statusCodigo === 'CONFIRMADO') {
            return {
                titulo: 'Ordem de visita aprovada e confirmada',
                aviso: `<div class="ticket-notice confirmed">
                    <strong>Atendimento confirmado pelo Leonardo.</strong> A visita técnica está agendada para a data e janela informadas abaixo.${a.observacaoTecnica ? ` Parecer: <em>${PP.esc(a.observacaoTecnica)}</em>` : ''}
                </div>`
            };
        }
        if (a.statusCodigo === 'RECUSADO') {
            return {
                titulo: 'Solicitação não aprovada para este horário',
                aviso: `<div class="ticket-notice refused">
                    <strong>Indisponibilidade no horário solicitado.</strong> ${a.observacaoTecnica ? `Justificativa registrada pelo Leonardo: <em>${PP.esc(a.observacaoTecnica)}</em>.` : ''} Utilize o botão abaixo para selecionar outra janela disponível.
                </div>`
            };
        }
        if (a.statusCodigo === 'CANCELADO') {
            return {
                titulo: 'Solicitação cancelada',
                aviso: `<div class="ticket-notice refused">Este atendimento foi cancelado e a respectiva janela de horário foi liberada na agenda.</div>`
            };
        }
        return {
            titulo: 'Guia de atendimento elétrico',
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
                    <div class="page-eyebrow" style="margin-bottom:6px">Guia de Atendimento Técnico</div>
                    <h1 style="font-size:1.25rem;font-weight:700;color:var(--navy-900);margin-bottom:8px">${info.titulo}</h1>
                    <span class="status-badge st-${a.statusCodigo}">${PP.esc(a.statusDescricao)}</span>
                </div>
                <div class="ticket-protocol-box">
                    <div class="kv-label">Protocolo nº</div>
                    <div class="code-ref" style="font-size:1.05rem;font-weight:600;color:var(--navy-900)">${PP.esc(a.protocolo)}</div>
                </div>
            </div>

            <div class="ticket-body">
                ${info.aviso}
                <div class="ticket-kv-grid">
                    <div class="ticket-kv-cell">
                        <div class="kv-label">Data e janela programada</div>
                        <div class="kv-val mono">${PP.formatarDataBr(a.data)} · ${a.horaInicio} às ${a.horaFim}</div>
                    </div>
                    <div class="ticket-kv-cell">
                        <div class="kv-label">Natureza do serviço</div>
                        <div class="kv-val">${PP.esc(a.tipoServicoDescricao)}</div>
                    </div>
                    <div class="ticket-kv-cell">
                        <div class="kv-label">Solicitante / Imóvel</div>
                        <div class="kv-val">${PP.esc(a.clienteNome)} (${PP.esc(a.clientePerfil)})</div>
                    </div>
                    <div class="ticket-kv-cell">
                        <div class="kv-label">Telefone de contato</div>
                        <div class="kv-val mono">${PP.esc(a.clienteTelefone)}</div>
                    </div>
                    <div class="ticket-kv-cell span-full">
                        <div class="kv-label">Endereço de execução</div>
                        <div class="kv-val">${PP.esc(a.enderecoAtendimento)}</div>
                    </div>
                    ${a.descricaoServico ? `
                        <div class="ticket-kv-cell span-full">
                            <div class="kv-label">Relato registrado na abertura</div>
                            <div class="kv-val" style="font-weight:400">${PP.esc(a.descricaoServico)}</div>
                        </div>
                    ` : ''}
                </div>
            </div>

            <div class="ticket-actions">
                <div style="display:flex;gap:8px;flex-wrap:wrap">
                    <button type="button" class="btn btn-secondary btn-sm" id="btnCopiarProt">Copiar protocolo</button>
                    <button type="button" class="btn btn-secondary btn-sm" onclick="window.print()">Imprimir guia</button>
                </div>
                <div style="display:flex;gap:8px;flex-wrap:wrap">
                    <a href="acompanhar.html?protocolo=${encodeURIComponent(a.protocolo)}" class="btn btn-secondary btn-sm">Consultar ou remarcar</a>
                    <a href="index.html" class="btn btn-primary btn-sm">${a.statusCodigo === 'RECUSADO' ? 'Escolher novo horário' : 'Nova solicitação'}</a>
                </div>
            </div>
        `;

        document.getElementById('btnCopiarProt').addEventListener('click', () => {
            navigator.clipboard?.writeText(a.protocolo);
            PP.toast(`Protocolo ${a.protocolo} copiado para a área de transferência.`);
        });
    } catch (err) {
        mount.innerHTML = `
            <div class="ticket-body">
                <h2 style="font-size:1.1rem;margin-bottom:8px;color:var(--navy-900)">Protocolo não localizado</h2>
                <p class="hint" style="margin-bottom:16px">${PP.esc(err.message)}</p>
                <a href="acompanhar.html" class="btn btn-secondary">Voltar à consulta</a>
            </div>
        `;
    }
});

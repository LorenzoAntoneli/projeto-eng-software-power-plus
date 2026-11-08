document.addEventListener('DOMContentLoaded', async () => {
    if (!(await PP.initBackoffice('mensagens'))) return;

    const mountLista = document.getElementById('mountListaConversas');
    const mountDetalhe = document.getElementById('mountDetalheConversa');
    let selecionadaId = null;

    function renderDetalhe(c) {
        mountDetalhe.innerHTML = `
            <div style="display:flex;justify-content:space-between;align-items:center;padding-bottom:14px;border-bottom:1px solid var(--border);margin-bottom:14px">
                <div>
                    <strong style="font-size:0.96rem">${PP.esc(c.nomeCliente)}</strong>
                    <span class="hint mono" style="margin-left:8px">${PP.esc(c.telefoneCliente || 'Sem telefone')}</span>
                </div>
                <span class="status-badge st-${c.statusCodigo}">${PP.esc(c.statusDescricao)}</span>
            </div>

            <div class="chat-stream" id="adminChatStream" style="padding:6px 4px;max-height:330px;margin-bottom:14px">
                ${c.mensagens.map(m => {
                    const cls = m.remetente === 'CLIENTE' ? 'theirs' : (m.remetente === 'PROPRIETARIO' ? 'mine' : 'owner-reply');
                    const autor = m.remetente === 'CLIENTE'
                        ? c.nomeCliente
                        : (m.remetente === 'PROPRIETARIO' ? 'Você (Leonardo)' : 'Triagem Automática');
                    return `
                        <div class="chat-msg ${cls}">
                            <div class="chat-author">${PP.esc(autor)}</div>
                            <div class="chat-bubble">${PP.esc(m.conteudo)}</div>
                        </div>
                    `;
                }).join('')}
            </div>

            <form id="formRespostaOwner" style="margin-top:auto;padding-top:12px;border-top:1px solid var(--border)">
                <div class="field">
                    <label for="txtRespostaHumana">Responder diretamente ao cliente</label>
                    <textarea id="txtRespostaHumana" rows="2" required placeholder="Escreva sua orientação técnica, orçamento ou confirmação..."></textarea>
                </div>
                <div style="display:flex;justify-content:space-between;align-items:center;margin-top:10px">
                    <label style="display:inline-flex;align-items:center;gap:6px;font-size:0.8rem;color:var(--ink-secondary);cursor:pointer">
                        <input type="checkbox" id="chkEncerrarConversa" style="width:auto">
                        <span>Encerrar atendimento após enviar</span>
                    </label>
                    <button type="submit" class="btn btn-primary btn-sm">Enviar resposta</button>
                </div>
            </form>
        `;

        const sc = document.getElementById('adminChatStream');
        sc.scrollTop = sc.scrollHeight;

        document.getElementById('formRespostaOwner').addEventListener('submit', async ev => {
            ev.preventDefault();
            try {
                await PP.api(`/api/admin/conversas/${c.id}/responder`, {
                    method: 'POST',
                    body: JSON.stringify({
                        resposta: document.getElementById('txtRespostaHumana').value,
                        encerrar: document.getElementById('chkEncerrarConversa').checked
                    })
                });
                PP.toast('Resposta enviada ao cliente.');
                await carregarConversas();
                await PP.refreshSidebarCounters();
            } catch (err) {
                PP.toast(err.message, true);
            }
        });
    }

    async function carregarConversas(preservarFoco = false) {
        const conversas = await PP.api('/api/admin/conversas');
        if (!conversas.length) {
            mountLista.innerHTML = '<div style="padding:24px;font-size:0.84rem;color:var(--ink-muted)">Nenhuma conversa registrada.</div>';
            return;
        }

        if (!selecionadaId) {
            selecionadaId = conversas[0].id;
        }

        mountLista.innerHTML = conversas.map(c => `
            <div class="inbox-row ${c.id === selecionadaId ? 'active' : ''}" data-conv="${c.id}">
                <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px">
                    <strong style="font-size:0.88rem">${PP.esc(c.nomeCliente)}</strong>
                    <span class="status-badge st-${c.statusCodigo}" style="font-size:0.68rem;padding:1px 7px">${PP.esc(c.statusDescricao)}</span>
                </div>
                <div class="hint" style="font-size:0.77rem">${PP.esc(c.telefoneCliente || 'Visitante web')} · ${c.mensagens.length} msg</div>
            </div>
        `).join('');

        mountLista.querySelectorAll('[data-conv]').forEach(row => {
            row.addEventListener('click', () => {
                selecionadaId = Number(row.dataset.conv);
                carregarConversas(false);
            });
        });

        const ativa = conversas.find(x => x.id === selecionadaId);
        const digitando = preservarFoco && document.activeElement && document.activeElement.id === 'txtRespostaHumana';
        if (ativa && !digitando) {
            renderDetalhe(ativa);
        }
    }

    await carregarConversas(false);

    setInterval(() => {
        carregarConversas(true).catch(() => {});
        PP.refreshSidebarCounters();
    }, 5000);
});


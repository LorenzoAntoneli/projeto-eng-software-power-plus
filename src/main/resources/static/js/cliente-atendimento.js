document.addEventListener('DOMContentLoaded', async () => {
    const stream = document.getElementById('chatMessagesStream');
    const statusPill = document.getElementById('chatStatusPill');
    const inpNome = document.getElementById('chatNomeCliente');
    const inpTel = document.getElementById('chatTelefoneCliente');
    const inpMsg = document.getElementById('chatInputMsg');

    let conversaId = sessionStorage.getItem('pp_chat_conversa_id')
        ? Number(sessionStorage.getItem('pp_chat_conversa_id'))
        : null;
    let ultimaQtdMensagens = 0;

    inpNome.value = sessionStorage.getItem('pp_chat_nome') || '';
    inpTel.value = sessionStorage.getItem('pp_chat_tel') || '';

    function renderizarConversa(conv) {
        conversaId = conv.id;
        sessionStorage.setItem('pp_chat_conversa_id', String(conv.id));

        statusPill.textContent = conv.statusDescricao;
        statusPill.className = `status-badge st-${conv.statusCodigo}`;

        if (conv.mensagens.length === ultimaQtdMensagens) return;
        ultimaQtdMensagens = conv.mensagens.length;

        stream.innerHTML = conv.mensagens.map(m => {
            const cls = m.remetente === 'CLIENTE' ? 'mine' : (m.remetente === 'PROPRIETARIO' ? 'owner-reply' : 'theirs');
            const autor = m.remetente === 'CLIENTE'
                ? 'Você'
                : (m.remetente === 'PROPRIETARIO' ? 'Leonardo · Eletricista' : 'Atendimento Power Plus');
            return `
                <div class="chat-msg ${cls}">
                    <div class="chat-author">${autor}</div>
                    <div class="chat-bubble">${PP.esc(m.conteudo)}</div>
                </div>
            `;
        }).join('');
        stream.scrollTop = stream.scrollHeight;
    }

    async function enviar(texto, solicitarHumano = false) {
        const nomeCliente = inpNome.value.trim() || 'Visitante Online';
        const telefoneCliente = inpTel.value.trim();
        sessionStorage.setItem('pp_chat_nome', inpNome.value.trim());
        sessionStorage.setItem('pp_chat_tel', telefoneCliente);

        try {
            const conv = await PP.api('/api/publico/chat/mensagens', {
                method: 'POST',
                body: JSON.stringify({
                    conversaId,
                    nomeCliente,
                    telefoneCliente,
                    mensagem: texto,
                    solicitarHumano
                })
            });
            renderizarConversa(conv);
        } catch (err) {
            PP.toast(err.message, true);
        }
    }

    document.getElementById('formChatCliente').addEventListener('submit', async ev => {
        ev.preventDefault();
        const txt = inpMsg.value.trim();
        if (!txt) return;
        inpMsg.value = '';
        await enviar(txt, false);
    });

    document.querySelectorAll('.topic-button').forEach(btn => {
        btn.addEventListener('click', () => enviar(btn.dataset.prompt, false));
    });

    document.getElementById('btnSolicitarEletricista').addEventListener('click', () => {
        enviar('Gostaria de falar diretamente com o eletricista Leonardo sobre minha demanda.', true);
    });

    document.getElementById('btnReiniciarChat').addEventListener('click', () => {
        conversaId = null;
        ultimaQtdMensagens = 0;
        sessionStorage.removeItem('pp_chat_conversa_id');
        statusPill.textContent = 'Triagem automática';
        statusPill.className = 'status-badge st-AUTOMATICO';
        stream.innerHTML = `
            <div class="chat-msg theirs">
                <div class="chat-author">Atendimento Power Plus</div>
                <div class="chat-bubble">Nova conversa iniciada. Como podemos ajudar?</div>
            </div>
        `;
    });

    if (conversaId) {
        try {
            const conv = await PP.api(`/api/publico/chat/${conversaId}`);
            renderizarConversa(conv);
        } catch (_) {
            conversaId = null;
            sessionStorage.removeItem('pp_chat_conversa_id');
        }
    }

    // Atualização em tempo real caso o proprietário responda no painel (/painel/mensagens)
    setInterval(async () => {
        if (!conversaId) return;
        try {
            const conv = await PP.api(`/api/publico/chat/${conversaId}`);
            renderizarConversa(conv);
        } catch (_) {}
    }, 4000);
});


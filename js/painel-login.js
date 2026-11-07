document.addEventListener('DOMContentLoaded', () => {
    if (PP.getToken()) {
        window.location.replace('agenda.html');
        return;
    }

    document.getElementById('formLoginPainel').addEventListener('submit', async ev => {
        ev.preventDefault();
        const btn = document.getElementById('btnEntrarPainel');
        btn.disabled = true;
        btn.textContent = 'Autenticando...';
        try {
            const res = await PP.api('/api/admin/auth/login', {
                method: 'POST',
                body: JSON.stringify({
                    username: document.getElementById('loginUser').value.trim(),
                    password: document.getElementById('loginPass').value
                })
            });
            PP.setSession(res.token, res.nomeExibicao);
            window.location.href = 'agenda.html';
        } catch (err) {
            PP.toast(err.message, true);
            btn.disabled = false;
            btn.textContent = 'Entrar no painel';
        }
    });
});


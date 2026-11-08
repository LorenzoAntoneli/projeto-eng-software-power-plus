window.PP = (() => {
    const DIAS_PT = {
        MONDAY: 'Segunda-feira',
        TUESDAY: 'Terça-feira',
        WEDNESDAY: 'Quarta-feira',
        THURSDAY: 'Quinta-feira',
        FRIDAY: 'Sexta-feira',
        SATURDAY: 'Sábado',
        SUNDAY: 'Domingo'
    };

    const MESES_PT = [
        'janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
        'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'
    ];

    const DIAS_CURTOS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

    function getToken() {
        return localStorage.getItem('pp_owner_token');
    }

    function setSession(token, nomeExibicao) {
        localStorage.setItem('pp_owner_token', token);
        localStorage.setItem('pp_owner_name', nomeExibicao || 'Leonardo');
    }

    function clearSession() {
        localStorage.removeItem('pp_owner_token');
        localStorage.removeItem('pp_owner_name');
    }

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

    function formatarDataExtenso(isoDate) {
        if (!isoDate) return '—';
        const d = new Date(isoDate + 'T12:00:00');
        return `${DIAS_CURTOS[d.getDay()]}, ${d.getDate()} de ${MESES_PT[d.getMonth()]} de ${d.getFullYear()}`;
    }

    function esc(str) {
        if (str == null) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function ensureToastNode() {
        let node = document.getElementById('ppToast');
        if (!node) {
            node = document.createElement('div');
            node.id = 'ppToast';
            node.className = 'toast hidden';
            document.body.appendChild(node);
        }
        return node;
    }

    function toast(msg, isError = false) {
        const box = ensureToastNode();
        box.textContent = msg;
        box.className = isError ? 'toast error' : 'toast';
        clearTimeout(box._timer);
        box._timer = setTimeout(() => box.classList.add('hidden'), 4200);
    }

    async function api(url, options = {}) {
        const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
        const token = getToken();
        if (token && url.startsWith('/api/admin')) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        const resp = await fetch(url, { ...options, headers });
        if (resp.status === 204) return null;
        const data = await resp.json().catch(() => ({}));
        if (!resp.ok) {
            if (resp.status === 401 && url.startsWith('/api/admin') && !url.endsWith('/login')) {
                clearSession();
                window.location.href = '/painel/login';
                throw new Error('Sessão expirada. Faça login novamente.');
            }
            throw new Error(data.mensagem || `Erro ${resp.status}`);
        }
        return data;
    }

    function openDialog({ title, description = '', inputLabel = null, defaultValue = '', placeholder = '', confirmLabel = 'Confirmar' }) {
        return new Promise(resolve => {
            let overlay = document.getElementById('ppSharedModal');
            if (overlay) overlay.remove();

            overlay = document.createElement('div');
            overlay.id = 'ppSharedModal';
            overlay.className = 'modal-overlay';
            overlay.innerHTML = `
                <div class="modal-window">
                    <div class="modal-top">
                        <h3>${esc(title)}</h3>
                        <button type="button" class="modal-close" data-close>×</button>
                    </div>
                    ${description ? `<p style="font-size:0.86rem;color:var(--ink-secondary);margin-bottom:14px">${esc(description)}</p>` : ''}
                    <form id="ppDialogForm">
                        ${inputLabel !== null ? `
                            <div class="field">
                                <label>${esc(inputLabel)}</label>
                                <input type="text" id="ppDialogInput" value="${esc(defaultValue)}" placeholder="${esc(placeholder)}">
                            </div>
                        ` : ''}
                        <div class="modal-actions-row">
                            <button type="button" class="btn btn-secondary" data-close>Cancelar</button>
                            <button type="submit" class="btn btn-primary">${esc(confirmLabel)}</button>
                        </div>
                    </form>
                </div>
            `;
            document.body.appendChild(overlay);

            const inp = overlay.querySelector('#ppDialogInput');
            if (inp) inp.focus();

            const close = (val) => {
                overlay.remove();
                resolve(val);
            };

            overlay.querySelectorAll('[data-close]').forEach(b => b.addEventListener('click', () => close(null)));
            overlay.querySelector('#ppDialogForm').addEventListener('submit', ev => {
                ev.preventDefault();
                close(inp ? inp.value : true);
            });
        });
    }

    async function initBackoffice(activeSlug) {
        if (!getToken()) {
            window.location.replace('/painel/login');
            return false;
        }

        const host = document.getElementById('boSidebarMount');
        if (host) {
            const ownerName = localStorage.getItem('pp_owner_name') || 'Leonardo';
            host.innerHTML = `
                <div class="bo-brand">
                    <img src="/img/logo-powerplus-light.svg" alt="Power Plus Gestão Operacional">
                </div>

                <div class="bo-section-label">Operação de Campo</div>
                <nav class="bo-nav" aria-label="Navegação operacional">
                    <a href="/painel/agenda" class="bo-link ${activeSlug === 'agenda' ? 'active' : ''}">
                        <span>Agenda e aprovações</span>
                        <span id="boCountAprovacoes" class="bo-badge">0</span>
                    </a>
                    <a href="/painel/expediente" class="bo-link ${activeSlug === 'expediente' ? 'active' : ''}">
                        <span>Expediente e bloqueios</span>
                    </a>
                    <a href="/painel/clientes" class="bo-link ${activeSlug === 'clientes' ? 'active' : ''}">
                        <span>Cadastro de clientes</span>
                    </a>
                </nav>

                <div class="bo-section-label" style="margin-top:8px">Relacionamento</div>
                <nav class="bo-nav" aria-label="Navegação de comunicação">
                    <a href="/painel/mensagens" class="bo-link ${activeSlug === 'mensagens' ? 'active' : ''}">
                        <span>Mensagens recebidas</span>
                        <span id="boCountChats" class="bo-badge">0</span>
                    </a>
                    <a href="/painel/avisos" class="bo-link ${activeSlug === 'avisos' ? 'active' : ''}">
                        <span>Registro de avisos</span>
                        <span id="boCountAvisos" class="bo-badge">0</span>
                    </a>
                </nav>

                <div class="bo-footer">
                    <div class="bo-user-row">
                        <strong>${esc(ownerName)} · Responsável Técnico</strong>
                        <span id="boCanalStatus">Notificações internas ativas</span>
                    </div>
                    <div class="bo-footer-links">
                        <a href="/" target="_blank">Portal do cliente</a>
                        <button type="button" id="boBtnLogout">Encerrar sessão</button>
                    </div>
                </div>
            `;

            document.getElementById('boBtnLogout').addEventListener('click', async () => {
                await api('/api/admin/auth/logout', { method: 'POST' }).catch(() => {});
                clearSession();
                window.location.href = '/painel/login';
            });
        }

        await refreshSidebarCounters();
        return true;
    }

    async function refreshSidebarCounters() {
        try {
            const dash = await api(`/api/admin/dashboard?data=${hojeIso()}`);
            const elAprov = document.getElementById('boCountAprovacoes');
            const elChats = document.getElementById('boCountChats');
            const elAvisos = document.getElementById('boCountAvisos');
            const elCanal = document.getElementById('boCanalStatus');
            if (elAprov) {
                const pend = dash.pendentesAprovacaoTotal ?? 0;
                elAprov.textContent = pend;
                elAprov.classList.toggle('has-alert', pend > 0);
            }
            if (elChats) {
                elChats.textContent = dash.chatsAguardandoHumano;
                elChats.classList.toggle('has-alert', dash.chatsAguardandoHumano > 0);
            }
            if (elAvisos) {
                elAvisos.textContent = dash.notificacoesNaoLidas;
                elAvisos.classList.toggle('has-alert', dash.notificacoesNaoLidas > 0);
            }
            if (elCanal) {
                elCanal.textContent = dash.whatsappFeatureFlagAtiva
                    ? 'Integração WhatsApp ativa'
                    : 'Notificações internas ativas';
            }
            return dash;
        } catch (_) {
            return null;
        }
    }

    return {
        DIAS_PT,
        getToken,
        setSession,
        clearSession,
        hojeIso,
        amanhaUtilIso,
        formatarDataBr,
        formatarDataExtenso,
        esc,
        toast,
        api,
        openDialog,
        initBackoffice,
        refreshSidebarCounters
    };
})();

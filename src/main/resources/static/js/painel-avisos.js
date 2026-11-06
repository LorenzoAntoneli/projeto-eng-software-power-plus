document.addEventListener('DOMContentLoaded', async () => {
    if (!(await PP.initBackoffice('avisos'))) return;

    const mount = document.getElementById('mountTabelaAvisos');

    async function carregarAvisos() {
        const lista = await PP.api('/api/admin/notificacoes');
        if (!lista.length) {
            mount.innerHTML = '<div class="empty-placeholder">Nenhum registro de envio até o momento.</div>';
            return;
        }

        mount.innerHTML = `
            <table class="clean-table">
                <thead>
                    <tr>
                        <th>Destinatário</th>
                        <th>Assunto</th>
                        <th>Mensagem registrada</th>
                        <th>Canal</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    ${lista.map(n => `
                        <tr style="opacity:${n.lida ? '0.72' : '1'}">
                            <td><strong style="font-size:0.8rem">${PP.esc(n.destinatarioPapel)}</strong></td>
                            <td>
                                <strong>${PP.esc(n.titulo)}</strong>
                                ${n.protocoloReferencia ? `<div class="hint mono">${PP.esc(n.protocoloReferencia)}</div>` : ''}
                            </td>
                            <td>${PP.esc(n.mensagem)}</td>
                            <td><span class="mono" style="font-size:0.78rem">${PP.esc(n.canalEnvio)}</span></td>
                            <td>
                                <span class="status-badge ${n.lida ? 'st-CONCLUIDO' : 'st-EM_ATENDIMENTO'}">
                                    ${n.lida ? 'Lido' : 'Novo'}
                                </span>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    }

    document.getElementById('btnMarcarTodasLidas').addEventListener('click', async () => {
        await PP.api('/api/admin/notificacoes/marcar-lidas', { method: 'POST' });
        PP.toast('Todos os avisos foram marcados como lidos.');
        await carregarAvisos();
        await PP.refreshSidebarCounters();
    });

    await carregarAvisos();
});


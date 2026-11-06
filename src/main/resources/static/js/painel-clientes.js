document.addEventListener('DOMContentLoaded', async () => {
    if (!(await PP.initBackoffice('clientes'))) return;

    const mountTabela = document.getElementById('mountTabelaClientes');
    const mountHist = document.getElementById('mountHistoricoCliente');
    const boxForm = document.getElementById('boxFormCliente');
    const inputBusca = document.getElementById('inputBuscaCliente');

    async function listarClientes() {
        const termo = inputBusca.value.trim();
        const url = termo ? `/api/admin/clientes?termo=${encodeURIComponent(termo)}` : '/api/admin/clientes';
        const clientes = await PP.api(url);

        if (!clientes.length) {
            mountTabela.innerHTML = '<div class="empty-placeholder">Nenhum cliente encontrado.</div>';
            return;
        }

        mountTabela.innerHTML = `
            <table class="clean-table">
                <thead>
                    <tr>
                        <th>Cliente</th>
                        <th>Categoria</th>
                        <th>Telefone</th>
                        <th>Endereço principal</th>
                        <th style="text-align:right">Ações</th>
                    </tr>
                </thead>
                <tbody>
                    ${clientes.map(c => `
                        <tr>
                            <td>
                                <strong>${PP.esc(c.nome)}</strong>
                                ${c.observacoes ? `<div class="hint">${PP.esc(c.observacoes)}</div>` : ''}
                            </td>
                            <td><span class="status-badge st-CONCLUIDO">${PP.esc(c.perfilDescricao)}</span></td>
                            <td class="mono">${PP.esc(c.telefone)}</td>
                            <td>${PP.esc(c.enderecoPadrao)}</td>
                            <td style="text-align:right">
                                <button type="button" class="btn btn-secondary btn-sm" data-hist-id="${c.id}" data-hist-nome="${PP.esc(c.nome)}">Ver histórico</button>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;

        mountTabela.querySelectorAll('[data-hist-id]').forEach(btn => {
            btn.addEventListener('click', () => abrirHistorico(btn.dataset.histId, btn.dataset.histNome));
        });
    }

    async function abrirHistorico(id, nome) {
        const lista = await PP.api(`/api/admin/clientes/${id}/historico`);
        mountHist.classList.remove('hidden');
        mountHist.innerHTML = `
            <div class="box-header">
                <h2>Histórico de atendimentos — ${PP.esc(nome)} (${lista.length})</h2>
                <button type="button" class="btn btn-secondary btn-sm" id="btnFecharHist">Fechar</button>
            </div>
            ${!lista.length
                ? '<div class="empty-placeholder">Nenhuma visita registrada para este cliente.</div>'
                : lista.map(a => `
                    <div class="order-item" style="padding:12px 16px">
                        <div class="order-head" style="margin-bottom:4px">
                            <div>
                                <strong class="mono">${PP.esc(a.protocolo)}</strong>
                                <span class="hint mono" style="margin-left:8px">${PP.formatarDataBr(a.data)} · ${a.horaInicio}</span>
                            </div>
                            <span class="status-badge st-${a.statusCodigo}">${PP.esc(a.statusDescricao)}</span>
                        </div>
                        <div style="font-size:0.86rem">${PP.esc(a.tipoServicoDescricao)} — <span class="hint">${PP.esc(a.enderecoAtendimento)}</span></div>
                    </div>
                  `).join('')}
        `;
        document.getElementById('btnFecharHist').addEventListener('click', () => mountHist.classList.add('hidden'));
        mountHist.scrollIntoView({ behavior: 'smooth' });
    }

    document.getElementById('btnAbrirFormCliente').addEventListener('click', () => boxForm.classList.remove('hidden'));
    document.getElementById('btnFecharFormCliente').addEventListener('click', () => boxForm.classList.add('hidden'));
    document.getElementById('btnBuscarCliente').addEventListener('click', listarClientes);
    inputBusca.addEventListener('keydown', ev => { if (ev.key === 'Enter') listarClientes(); });

    document.getElementById('formNovoCliente').addEventListener('submit', async ev => {
        ev.preventDefault();
        try {
            await PP.api('/api/admin/clientes', {
                method: 'POST',
                body: JSON.stringify({
                    nome: document.getElementById('cliNome').value,
                    telefone: document.getElementById('cliTelefone').value,
                    perfil: document.getElementById('cliPerfil').value,
                    endereco: document.getElementById('cliEndereco').value,
                    email: document.getElementById('cliEmail').value,
                    observacoes: document.getElementById('cliObs').value
                })
            });
            PP.toast('Cliente cadastrado com sucesso.');
            document.getElementById('formNovoCliente').reset();
            boxForm.classList.add('hidden');
            await listarClientes();
        } catch (err) {
            PP.toast(err.message, true);
        }
    });

    await listarClientes();
});


document.addEventListener('DOMContentLoaded', () => {
    const inputProt = document.getElementById('inputBuscaProtocolo');
    const inputTel = document.getElementById('inputBuscaTelefone');
    const boxResultado = document.getElementById('resultadoConsulta');
    const modalRemarcar = document.getElementById('modalRemarcar');
    const remNovaData = document.getElementById('remNovaData');
    const remNovoHorario = document.getElementById('remNovoHorario');

    function renderOrdem(a, historico = null) {
        const alteravel = a.statusCodigo === 'CONFIRMADO';
        const histHtml = historico && historico.length
            ? `<div class="order-history">
                <strong style="color:var(--ink)">Registro de movimentações</strong>
                ${historico.map(h => `<div>${PP.esc(h.statusNovo)} — ${PP.esc(h.responsavel)}${h.observacao ? ` (${PP.esc(h.observacao)})` : ''}</div>`).join('')}
               </div>`
            : '';

        return `
            <div class="order-item">
                <div class="order-head">
                    <div style="display:flex;align-items:center;gap:10px">
                        <strong class="mono">${PP.esc(a.protocolo)}</strong>
                        <span class="hint mono">${PP.formatarDataBr(a.data)} · ${a.horaInicio}–${a.horaFim}</span>
                    </div>
                    <span class="status-badge st-${a.statusCodigo}">${PP.esc(a.statusDescricao)}</span>
                </div>
                <div class="order-meta">
                    <div><strong>Serviço:</strong> ${PP.esc(a.tipoServicoDescricao)}</div>
                    <div><strong>Cliente:</strong> ${PP.esc(a.clienteNome)} (${PP.esc(a.clientePerfil)})</div>
                    <div style="grid-column:1/-1"><strong>Endereço:</strong> ${PP.esc(a.enderecoAtendimento)}</div>
                    ${a.observacaoTecnica ? `<div style="grid-column:1/-1"><strong>Parecer técnico:</strong> ${PP.esc(a.observacaoTecnica)}</div>` : ''}
                </div>
                ${histHtml}
                <div class="order-foot">
                    <a href="comprovante.html?protocolo=${encodeURIComponent(a.protocolo)}" class="btn btn-secondary btn-sm">Ver comprovante</a>
                    ${alteravel ? `
                        <button type="button" class="btn btn-secondary btn-sm" data-remarcar="${PP.esc(a.protocolo)}" data-data="${a.data}">Remarcar horário</button>
                        <button type="button" class="btn btn-danger btn-sm" data-cancelar="${PP.esc(a.protocolo)}">Cancelar visita</button>
                    ` : ''}
                </div>
            </div>
        `;
    }

    function bindAcoesResultado() {
        boxResultado.querySelectorAll('[data-remarcar]').forEach(btn => {
            btn.addEventListener('click', () => abrirRemarcacao(btn.dataset.remarcar, btn.dataset.data));
        });
        boxResultado.querySelectorAll('[data-cancelar]').forEach(btn => {
            btn.addEventListener('click', () => cancelarVisita(btn.dataset.cancelar));
        });
    }

    async function buscarProtocolo(prot) {
        const codigo = (prot ?? inputProt.value).trim();
        if (!codigo) return PP.toast('Informe o protocolo para consultar.', true);
        inputProt.value = codigo;
        try {
            const res = await PP.api(`/api/publico/agendamentos/protocolo/${encodeURIComponent(codigo)}`);
            boxResultado.className = '';
            boxResultado.innerHTML = renderOrdem(res.agendamento, res.historico);
            bindAcoesResultado();
        } catch (err) {
            boxResultado.className = 'empty-placeholder';
            boxResultado.textContent = err.message;
        }
    }

    async function buscarTelefone(tel) {
        const fone = (tel ?? inputTel.value).trim();
        if (!fone) return PP.toast('Informe o telefone com DDD.', true);
        inputTel.value = fone;
        try {
            const lista = await PP.api(`/api/publico/agendamentos/cliente?telefone=${encodeURIComponent(fone)}`);
            if (!lista.length) {
                boxResultado.className = 'empty-placeholder';
                boxResultado.textContent = 'Nenhum atendimento localizado para este telefone.';
                return;
            }
            boxResultado.className = '';
            boxResultado.innerHTML = lista.map(a => renderOrdem(a)).join('');
            bindAcoesResultado();
        } catch (err) {
            boxResultado.className = 'empty-placeholder';
            boxResultado.textContent = err.message;
        }
    }

    async function carregarHorariosRemarcacao() {
        const res = await PP.api(`/api/publico/horarios-livres?data=${remNovaData.value}`);
        remNovoHorario.innerHTML = res.horariosLivres.length
            ? res.horariosLivres.map(h => `<option value="${h}">${h}</option>`).join('')
            : '<option value="">Sem horários vagos nesta data</option>';
    }

    async function abrirRemarcacao(protocolo, dataAtual) {
        document.getElementById('remProtocolo').value = protocolo;
        document.getElementById('remProtocoloTitulo').textContent = protocolo;
        remNovaData.min = PP.hojeIso();
        remNovaData.value = dataAtual >= PP.hojeIso() ? dataAtual : PP.amanhaUtilIso();
        await carregarHorariosRemarcacao();
        modalRemarcar.classList.remove('hidden');
    }

    function fecharRemarcacao() {
        modalRemarcar.classList.add('hidden');
    }

    async function cancelarVisita(protocolo) {
        const motivo = await PP.openDialog({
            title: `Cancelar visita ${protocolo}`,
            description: 'O horário será liberado imediatamente na agenda. Cancelamentos exigem antecedência mínima de 2 horas.',
            inputLabel: 'Motivo do cancelamento (opcional)',
            defaultValue: '',
            placeholder: 'Ex.: Imprevisto no trabalho',
            confirmLabel: 'Confirmar cancelamento'
        });
        if (motivo === null) return;
        try {
            await PP.api(`/api/publico/agendamentos/${encodeURIComponent(protocolo)}/cancelar`, {
                method: 'POST',
                body: JSON.stringify({ motivo })
            });
            PP.toast(`Visita ${protocolo} cancelada.`);
            await buscarProtocolo(protocolo);
        } catch (err) {
            PP.toast(err.message, true);
        }
    }

    document.getElementById('btnBuscarProtocolo').addEventListener('click', () => buscarProtocolo());
    document.getElementById('btnBuscarTelefone').addEventListener('click', () => buscarTelefone());
    inputProt.addEventListener('keydown', ev => { if (ev.key === 'Enter') buscarProtocolo(); });
    inputTel.addEventListener('keydown', ev => { if (ev.key === 'Enter') buscarTelefone(); });

    document.querySelectorAll('[data-prot]').forEach(b => b.addEventListener('click', () => buscarProtocolo(b.dataset.prot)));
    document.querySelectorAll('[data-tel]').forEach(b => b.addEventListener('click', () => buscarTelefone(b.dataset.tel)));

    remNovaData.addEventListener('change', carregarHorariosRemarcacao);
    document.getElementById('btnFecharRemarcar').addEventListener('click', fecharRemarcacao);
    document.getElementById('btnCancelarModalRemarcar').addEventListener('click', fecharRemarcacao);

    document.getElementById('formRemarcarCliente').addEventListener('submit', async ev => {
        ev.preventDefault();
        const protocolo = document.getElementById('remProtocolo').value;
        try {
            await PP.api(`/api/publico/agendamentos/${encodeURIComponent(protocolo)}/remarcar`, {
                method: 'PUT',
                body: JSON.stringify({
                    novaData: remNovaData.value,
                    novaHoraInicio: remNovoHorario.value,
                    motivo: document.getElementById('remMotivo').value
                })
            });
            fecharRemarcacao();
            PP.toast(`Horário do protocolo ${protocolo} atualizado.`);
            await buscarProtocolo(protocolo);
        } catch (err) {
            PP.toast(err.message, true);
        }
    });

    const urlProt = new URLSearchParams(window.location.search).get('protocolo');
    if (urlProt) {
        buscarProtocolo(urlProt);
    }
});


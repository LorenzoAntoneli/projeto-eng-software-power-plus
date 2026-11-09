document.addEventListener('DOMContentLoaded', () => {
    const inputProt = document.getElementById('inputBuscaProtocolo');
    const inputTel = document.getElementById('inputBuscaTelefone');
    const boxResultado = document.getElementById('resultadoConsulta');
    const modalRemarcar = document.getElementById('modalRemarcar');
    const remNovaData = document.getElementById('remNovaData');
    const remNovoHorario = document.getElementById('remNovoHorario');

    function aplicarMascaraTelefone(valor) {
        const digitos = String(valor || '').replace(/\D/g, '').slice(0, 11);
        if (digitos.length <= 2) return digitos;
        if (digitos.length <= 6) return `(${digitos.slice(0, 2)}) ${digitos.slice(2)}`;
        if (digitos.length <= 10) return `(${digitos.slice(0, 2)}) ${digitos.slice(2, 6)}-${digitos.slice(6)}`;
        return `(${digitos.slice(0, 2)}) ${digitos.slice(2, 7)}-${digitos.slice(7)}`;
    }

    if (inputTel) {
        inputTel.addEventListener('input', () => {
            inputTel.value = aplicarMascaraTelefone(inputTel.value);
        });
    }

    function renderOrdem(a, historico = null) {
        const alteravel = a.statusCodigo === 'CONFIRMADO' || a.statusCodigo === 'AGUARDANDO_APROVACAO';
        let resumoStatus = '';
        if (a.statusCodigo === 'AGUARDANDO_APROVACAO') {
            resumoStatus = '<div class="ticket-notice pending" style="margin-bottom:12px">Solicitação registrada e horário pré-reservado — aguardando aprovação do Leonardo.</div>';
        } else if (a.statusCodigo === 'CONFIRMADO') {
            resumoStatus = '<div class="ticket-notice confirmed" style="margin-bottom:12px">Visita técnica aprovada e confirmada pelo Leonardo.</div>';
        } else if (a.statusCodigo === 'RECUSADO') {
            resumoStatus = `<div class="ticket-notice refused" style="margin-bottom:12px">Solicitação recusada neste horário${a.observacaoTecnica ? `: ${PP.esc(a.observacaoTecnica)}` : '.'}</div>`;
        }
        const histHtml = historico && historico.length
            ? `<div class="order-history">
                <strong style="color:var(--ink)">Histórico de tramitação</strong>
                ${historico.map(h => `<div>${PP.esc(h.statusNovo)} — ${PP.esc(h.responsavel)}${h.observacao ? ` (${PP.esc(h.observacao)})` : ''}</div>`).join('')}
               </div>`
            : '';

        return `
            <div class="order-item os-${a.statusCodigo}">
                <div class="order-head">
                    <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
                        <strong class="code-ref">${PP.esc(a.protocolo)}</strong>
                        <span class="hint mono">${PP.formatarDataBr(a.data)} · ${a.horaInicio} às ${a.horaFim}</span>
                    </div>
                    <span class="status-badge st-${a.statusCodigo}">${PP.esc(a.statusDescricao)}</span>
                </div>
                ${resumoStatus}
                <div class="order-meta">
                    <div><strong>Serviço:</strong> ${PP.esc(a.tipoServicoDescricao)}</div>
                    <div><strong>Solicitante:</strong> ${PP.esc(a.clienteNome)} (${PP.esc(a.clientePerfil)})</div>
                    <div style="grid-column:1/-1"><strong>Endereço:</strong> ${PP.esc(a.enderecoAtendimento)}</div>
                    ${a.descricaoServico ? `<div style="grid-column:1/-1"><strong>Relato registrado:</strong> ${PP.esc(a.descricaoServico)}</div>` : ''}
                    ${a.observacaoTecnica ? `<div style="grid-column:1/-1"><strong>Parecer técnico:</strong> ${PP.esc(a.observacaoTecnica)}</div>` : ''}
                </div>
                ${histHtml}
                <div class="order-foot">
                    <a href="comprovante.html?protocolo=${encodeURIComponent(a.protocolo)}" class="btn btn-secondary btn-sm">Abrir guia</a>
                    ${alteravel ? `
                        <button type="button" class="btn btn-secondary btn-sm" data-remarcar="${PP.esc(a.protocolo)}" data-data="${a.data}">Remarcar horário</button>
                        <button type="button" class="btn btn-danger btn-sm" data-cancelar="${PP.esc(a.protocolo)}">Cancelar solicitação</button>
                    ` : ''}
                    ${a.statusCodigo === 'RECUSADO' ? `<a href="index.html" class="btn btn-primary btn-sm">Solicitar outro horário</a>` : ''}
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
        const codigo = (prot ?? inputProt.value).trim().toUpperCase();
        if (!codigo) return PP.toast('Informe o código de protocolo para busca.', true);
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
        inputTel.value = aplicarMascaraTelefone(fone) || fone;
        try {
            const lista = await PP.api(`/api/publico/agendamentos/cliente?telefone=${encodeURIComponent(fone)}`);
            if (!lista.length) {
                boxResultado.className = 'empty-placeholder';
                boxResultado.textContent = 'Nenhuma ordem localizada para o telefone informado.';
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
        if (!remNovaData.value) {
            remNovoHorario.innerHTML = '<option value="">Selecione uma data</option>';
            return;
        }
        try {
            const res = await PP.api(`/api/publico/horarios-livres?data=${remNovaData.value}`);
            remNovoHorario.innerHTML = res.horariosLivres.length
                ? res.horariosLivres.map(h => `<option value="${h}">${h}</option>`).join('')
                : '<option value="">Sem horários vagos nesta data</option>';
        } catch (_) {
            remNovoHorario.innerHTML = '<option value="">Sem horários vagos nesta data</option>';
        }
    }

    async function abrirRemarcacao(protocolo, dataAtual) {
        document.getElementById('remProtocolo').value = protocolo;
        document.getElementById('remProtocoloTitulo').textContent = protocolo;
        remNovaData.min = PP.hojeIso();
        remNovaData.value = dataAtual >= PP.hojeIso() ? dataAtual : PP.amanhaUtilIso();
        await carregarHorariosRemarcacao();
        if (!remNovoHorario.value && remNovaData.value === PP.hojeIso()) {
            remNovaData.value = PP.amanhaUtilIso();
            await carregarHorariosRemarcacao();
        }
        modalRemarcar.classList.remove('hidden');
    }

    function fecharRemarcacao() {
        modalRemarcar.classList.add('hidden');
    }

    async function cancelarVisita(protocolo) {
        const motivo = await PP.openDialog({
            title: `Cancelar protocolo ${protocolo}`,
            description: 'A janela de horário será liberada imediatamente na agenda. Cancelamentos exigem antecedência mínima de 2 horas.',
            inputLabel: 'Motivo do cancelamento (opcional)',
            defaultValue: '',
            placeholder: 'Ex.: Imprevisto no local',
            confirmLabel: 'Confirmar cancelamento'
        });
        if (motivo === null) return;
        try {
            await PP.api(`/api/publico/agendamentos/${encodeURIComponent(protocolo)}/cancelar`, {
                method: 'POST',
                body: JSON.stringify({ motivo })
            });
            PP.toast(`Solicitação ${protocolo} cancelada.`);
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

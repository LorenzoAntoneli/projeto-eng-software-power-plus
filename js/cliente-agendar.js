document.addEventListener('DOMContentLoaded', async () => {
    const selServico = document.getElementById('agTipoServico');
    const selPerfil = document.getElementById('agPerfil');
    const inputData = document.getElementById('agData');
    const slotsContainer = document.getElementById('slotsContainer');
    const hiddenHora = document.getElementById('agHoraInicio');
    const atalhosData = document.getElementById('atalhosData');
    const resumoEtapa2 = document.getElementById('resumoEtapa2');
    const inputTelefone = document.getElementById('agTelefone');

    const railServico = document.getElementById('railServico');
    const railData = document.getElementById('railData');
    const railHorario = document.getElementById('railHorario');

    const etapa1 = document.getElementById('etapa1');
    const etapa2 = document.getElementById('etapa2');
    const stepPill1 = document.getElementById('stepPill1');
    const stepPill2 = document.getElementById('stepPill2');

    function dataUtilOffset(diasExtras) {
        const d = new Date();
        let somados = 0;
        while (somados < diasExtras) {
            d.setDate(d.getDate() + 1);
            if (d.getDay() !== 0) somados++;
        }
        const off = d.getTimezoneOffset();
        return new Date(d.getTime() - off * 60000).toISOString().slice(0, 10);
    }

    function proximoSabadoIso() {
        const d = new Date();
        const falta = (6 - d.getDay() + 7) % 7 || 7;
        d.setDate(d.getDate() + falta);
        const off = d.getTimezoneOffset();
        return new Date(d.getTime() - off * 60000).toISOString().slice(0, 10);
    }

    function montarAtalhosData() {
        if (!atalhosData) return;
        const opcoes = [
            { rotulo: 'Amanhã', iso: dataUtilOffset(1) },
            { rotulo: 'Próximo dia útil', iso: dataUtilOffset(2) },
            { rotulo: 'Próximo sábado', iso: proximoSabadoIso() }
        ];
        atalhosData.innerHTML = opcoes
            .map(o => `<button type="button" class="date-chip ${inputData.value === o.iso ? 'active' : ''}" data-iso="${o.iso}">${o.rotulo} (${o.iso.slice(8, 10)}/${o.iso.slice(5, 7)})</button>`)
            .join('');
        atalhosData.querySelectorAll('.date-chip').forEach(btn => {
            btn.addEventListener('click', () => {
                inputData.value = btn.dataset.iso;
                montarAtalhosData();
                carregarHorarios();
            });
        });
    }

    function aplicarMascaraTelefone(valor) {
        const digitos = String(valor || '').replace(/\D/g, '').slice(0, 11);
        if (digitos.length <= 2) return digitos;
        if (digitos.length <= 6) return `(${digitos.slice(0, 2)}) ${digitos.slice(2)}`;
        if (digitos.length <= 10) return `(${digitos.slice(0, 2)}) ${digitos.slice(2, 6)}-${digitos.slice(6)}`;
        return `(${digitos.slice(0, 2)}) ${digitos.slice(2, 7)}-${digitos.slice(7)}`;
    }

    if (inputTelefone) {
        inputTelefone.addEventListener('input', () => {
            inputTelefone.value = aplicarMascaraTelefone(inputTelefone.value);
        });
    }

    function atualizarResumoLateral() {
        const opt = selServico.options[selServico.selectedIndex];
        const servicoTxt = opt ? opt.textContent : '—';
        railServico.textContent = servicoTxt;
        railData.textContent = PP.formatarDataExtenso(inputData.value);
        railHorario.textContent = hiddenHora.value ? `${hiddenHora.value} (60 min)` : 'Selecione um horário';
        if (resumoEtapa2 && hiddenHora.value) {
            resumoEtapa2.innerHTML = `
                <div>
                    <strong>Horário escolhido:</strong> <span class="mono">${PP.formatarDataBr(inputData.value)} às ${hiddenHora.value}</span>
                    · ${PP.esc(servicoTxt)}
                </div>
                <span class="status-badge st-AGUARDANDO_APROVACAO">Sujeito à aprovação do Leonardo</span>
            `;
        }
    }

    async function carregarHorarios() {
        hiddenHora.value = '';
        atualizarResumoLateral();
        if (!inputData.value) {
            slotsContainer.innerHTML = '<span class="hint">Selecione uma data acima.</span>';
            return;
        }
        try {
            const res = await PP.api(`/api/publico/horarios-livres?data=${inputData.value}`);
            if (!res.horariosLivres || res.horariosLivres.length === 0) {
                slotsContainer.innerHTML = '<div class="empty-placeholder" style="grid-column:1/-1;padding:18px">Sem horários vagos nesta data (fora de expediente, data bloqueada ou agenda completa). Use os atalhos acima para escolher outro dia.</div>';
                return;
            }
            slotsContainer.innerHTML = res.horariosLivres
                .map(h => `<button type="button" class="slot-btn" data-hora="${h}">${h}</button>`)
                .join('');

            slotsContainer.querySelectorAll('.slot-btn').forEach(btn => {
                btn.addEventListener('click', () => {
                    slotsContainer.querySelectorAll('.slot-btn').forEach(b => b.classList.remove('selected'));
                    btn.classList.add('selected');
                    hiddenHora.value = btn.dataset.hora;
                    document.getElementById('msgEtapa1').textContent = `Horário ${btn.dataset.hora} selecionado — clique em Continuar.`;
                    atualizarResumoLateral();
                });
            });
        } catch (err) {
            slotsContainer.innerHTML = `<span class="hint">${PP.esc(err.message)}</span>`;
        }
    }

    function irParaEtapa(num) {
        if (num === 2) {
            if (!inputData.value) return PP.toast('Selecione a data da visita.', true);
            if (!hiddenHora.value) return PP.toast('Escolha um dos horários disponíveis antes de continuar.', true);
            atualizarResumoLateral();
            etapa1.classList.add('hidden');
            etapa2.classList.remove('hidden');
            stepPill1.className = 'step-pill done';
            stepPill2.className = 'step-pill active';
            document.getElementById('agNome').focus();
        } else {
            etapa2.classList.add('hidden');
            etapa1.classList.remove('hidden');
            stepPill1.className = 'step-pill active';
            stepPill2.className = 'step-pill';
        }
    }

    try {
        const cat = await PP.api('/api/publico/catalogo');
        selServico.innerHTML = cat.tiposServico
            .map(s => `<option value="${s.codigo}">${PP.esc(s.descricao)}</option>`)
            .join('');
        selPerfil.innerHTML = cat.perfisCliente
            .map(p => `<option value="${p.codigo}">${PP.esc(p.descricao)}</option>`)
            .join('');

        inputData.min = PP.hojeIso();
        inputData.value = PP.amanhaUtilIso();
        montarAtalhosData();
        await carregarHorarios();
    } catch (err) {
        PP.toast(err.message, true);
    }

    selServico.addEventListener('change', atualizarResumoLateral);
    inputData.addEventListener('change', () => {
        montarAtalhosData();
        carregarHorarios();
    });
    document.getElementById('btnAvancarEtapa2').addEventListener('click', () => irParaEtapa(2));
    document.getElementById('btnVoltarEtapa1').addEventListener('click', () => irParaEtapa(1));

    document.getElementById('formAgendamento').addEventListener('submit', async ev => {
        ev.preventDefault();
        const btn = document.getElementById('btnSubmitReserva');
        btn.disabled = true;
        btn.textContent = 'Enviando solicitação...';
        try {
            const criado = await PP.api('/api/publico/agendamentos', {
                method: 'POST',
                body: JSON.stringify({
                    nomeCliente: document.getElementById('agNome').value,
                    telefone: document.getElementById('agTelefone').value,
                    email: document.getElementById('agEmail').value,
                    endereco: document.getElementById('agEndereco').value,
                    perfilCliente: selPerfil.value,
                    data: inputData.value,
                    horaInicio: hiddenHora.value,
                    tipoServico: selServico.value,
                    descricaoServico: document.getElementById('agDescricao').value
                })
            });
            window.location.href = `comprovante.html?protocolo=${encodeURIComponent(criado.protocolo)}`;
        } catch (err) {
            PP.toast(err.message, true);
            btn.disabled = false;
            btn.textContent = 'Solicitar agendamento';
        }
    });
});

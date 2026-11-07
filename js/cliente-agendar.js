document.addEventListener('DOMContentLoaded', async () => {
    const selServico = document.getElementById('agTipoServico');
    const selPerfil = document.getElementById('agPerfil');
    const inputData = document.getElementById('agData');
    const slotsContainer = document.getElementById('slotsContainer');
    const hiddenHora = document.getElementById('agHoraInicio');

    const railServico = document.getElementById('railServico');
    const railData = document.getElementById('railData');
    const railHorario = document.getElementById('railHorario');

    const etapa1 = document.getElementById('etapa1');
    const etapa2 = document.getElementById('etapa2');
    const stepPill1 = document.getElementById('stepPill1');
    const stepPill2 = document.getElementById('stepPill2');

    function atualizarResumoLateral() {
        const opt = selServico.options[selServico.selectedIndex];
        railServico.textContent = opt ? opt.textContent : '—';
        railData.textContent = PP.formatarDataExtenso(inputData.value);
        railHorario.textContent = hiddenHora.value ? `${hiddenHora.value} (60 min)` : 'Selecione um horário';
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
                slotsContainer.innerHTML = '<div class="empty-placeholder" style="grid-column:1/-1;padding:18px">Sem horários vagos nesta data (fora de expediente, data bloqueada ou agenda completa). Escolha outra data.</div>';
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
                    document.getElementById('msgEtapa1').textContent = `Horário ${btn.dataset.hora} selecionado.`;
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
        await carregarHorarios();
    } catch (err) {
        PP.toast(err.message, true);
    }

    selServico.addEventListener('change', atualizarResumoLateral);
    inputData.addEventListener('change', carregarHorarios);
    document.getElementById('btnAvancarEtapa2').addEventListener('click', () => irParaEtapa(2));
    document.getElementById('btnVoltarEtapa1').addEventListener('click', () => irParaEtapa(1));

    document.getElementById('formAgendamento').addEventListener('submit', async ev => {
        ev.preventDefault();
        const btn = document.getElementById('btnSubmitReserva');
        btn.disabled = true;
        btn.textContent = 'Reservando horário...';
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
            btn.textContent = 'Confirmar agendamento';
        }
    });
});


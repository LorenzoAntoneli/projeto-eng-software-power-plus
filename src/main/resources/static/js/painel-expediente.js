document.addEventListener('DOMContentLoaded', async () => {
    if (!(await PP.initBackoffice('expediente'))) return;

    const mountDisp = document.getElementById('mountDisponibilidade');
    const mountBloq = document.getElementById('mountBloqueios');

    async function carregarConfiguracao() {
        const cfg = await PP.api('/api/admin/disponibilidade');

        mountDisp.innerHTML = cfg.disponibilidades.map(d => `
            <div class="weekday-row">
                <strong>${PP.DIAS_PT[d.diaSemana] || d.diaSemana}</strong>
                <label style="display:inline-flex;align-items:center;gap:6px;font-size:0.82rem;cursor:pointer">
                    <input type="checkbox" id="chk_${d.diaSemana}" ${d.ativo ? 'checked' : ''} style="width:auto">
                    <span>Ativo</span>
                </label>
                <div class="time-range-inputs">
                    <input type="time" id="ini_${d.diaSemana}" value="${d.horaInicio.slice(0, 5)}">
                    <span class="hint">às</span>
                    <input type="time" id="fim_${d.diaSemana}" value="${d.horaFim.slice(0, 5)}">
                </div>
                <button type="button" class="btn btn-secondary btn-sm" data-save-dia="${d.diaSemana}" data-dur="${d.duracaoSlotMinutos}">Salvar</button>
            </div>
        `).join('');

        mountDisp.querySelectorAll('[data-save-dia]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const dia = btn.dataset.saveDia;
                try {
                    await PP.api('/api/admin/disponibilidade', {
                        method: 'PUT',
                        body: JSON.stringify({
                            diaSemana: dia,
                            ativo: document.getElementById(`chk_${dia}`).checked,
                            horaInicio: document.getElementById(`ini_${dia}`).value,
                            horaFim: document.getElementById(`fim_${dia}`).value,
                            duracaoSlotMinutos: Number(btn.dataset.dur)
                        })
                    });
                    PP.toast(`Expediente de ${PP.DIAS_PT[dia]} salvo.`);
                } catch (err) {
                    PP.toast(err.message, true);
                }
            });
        });

        if (!cfg.bloqueios.length) {
            mountBloq.innerHTML = '<div class="empty-placeholder" style="padding:20px">Nenhuma data bloqueada no momento.</div>';
        } else {
            mountBloq.innerHTML = cfg.bloqueios.map(b => `
                <div class="order-item" style="display:flex;justify-content:space-between;align-items:center;padding:11px 14px">
                    <div>
                        <strong class="mono">${PP.formatarDataBr(b.data)}</strong>
                        <span class="hint" style="margin-left:8px">${PP.esc(b.motivo)}</span>
                    </div>
                    <button type="button" class="btn btn-danger btn-sm" data-del-bloq="${b.id}">Remover</button>
                </div>
            `).join('');

            mountBloq.querySelectorAll('[data-del-bloq]').forEach(btn => {
                btn.addEventListener('click', async () => {
                    await PP.api(`/api/admin/bloqueios/${btn.dataset.delBloq}`, { method: 'DELETE' });
                    PP.toast('Bloqueio removido.');
                    await carregarConfiguracao();
                });
            });
        }
    }

    async function submeterBloqueio(forcar = false) {
        const data = document.getElementById('bloqData').value;
        const motivo = document.getElementById('bloqMotivo').value;
        try {
            await PP.api('/api/admin/bloqueios', {
                method: 'POST',
                body: JSON.stringify({ data, motivo, forcarComConflito: forcar })
            });
            PP.toast('Data bloqueada na agenda.');
            document.getElementById('formBloqueioData').reset();
            await carregarConfiguracao();
        } catch (err) {
            if (!forcar && err.message.includes('Confirme o bloqueio')) {
                const ok = await PP.openDialog({
                    title: 'Atenção: existem visitas agendadas',
                    description: err.message,
                    inputLabel: null,
                    confirmLabel: 'Bloquear mesmo assim'
                });
                if (ok) await submeterBloqueio(true);
            } else {
                PP.toast(err.message, true);
            }
        }
    }

    document.getElementById('formBloqueioData').addEventListener('submit', ev => {
        ev.preventDefault();
        submeterBloqueio(false);
    });

    await carregarConfiguracao();
});


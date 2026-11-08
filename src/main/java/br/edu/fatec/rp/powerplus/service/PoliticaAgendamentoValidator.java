package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.Agendamento;
import br.edu.fatec.rp.powerplus.domain.DisponibilidadeSemanal;
import br.edu.fatec.rp.powerplus.domain.StatusAgendamento;
import br.edu.fatec.rp.powerplus.domain.exception.ConflitoHorarioException;
import br.edu.fatec.rp.powerplus.domain.exception.RegraNegocioException;
import br.edu.fatec.rp.powerplus.repository.AgendamentoRepository;
import br.edu.fatec.rp.powerplus.repository.BloqueioAgendaRepository;
import br.edu.fatec.rp.powerplus.repository.DisponibilidadeSemanalRepository;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.List;

/**
 * Componente extraído na etapa REFACTOR do ciclo TDD para centralizar
 * as políticas RN01, RN02, RN03 e RN04 sem duplicação entre agendamento e remarcação.
 */
@Component
public class PoliticaAgendamentoValidator {

    public static final long ANTECEDENCIA_MINIMA_MINUTOS = 120L;
    public static final List<StatusAgendamento> STATUS_QUE_OCUPAM_AGENDA =
            List.of(StatusAgendamento.AGUARDANDO_APROVACAO, StatusAgendamento.CONFIRMADO, StatusAgendamento.EM_ATENDIMENTO);

    private final AgendamentoRepository agendamentoRepository;
    private final DisponibilidadeSemanalRepository disponibilidadeRepository;
    private final BloqueioAgendaRepository bloqueioRepository;
    private final Clock clock;

    public PoliticaAgendamentoValidator(AgendamentoRepository agendamentoRepository,
                                        DisponibilidadeSemanalRepository disponibilidadeRepository,
                                        BloqueioAgendaRepository bloqueioRepository,
                                        Clock clock) {
        this.agendamentoRepository = agendamentoRepository;
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.clock = clock;
    }

    public LocalTime validarHorarioDisponivelECalcularFim(LocalDate data, LocalTime horaInicio, Long ignorarAgendamentoId) {
        if (data == null || horaInicio == null) {
            throw new RegraNegocioException("A data e o horário de início são obrigatórios.");
        }
        LocalDateTime agora = LocalDateTime.now(clock);
        if (!LocalDateTime.of(data, horaInicio).isAfter(agora)) {
            throw new RegraNegocioException("O agendamento deve ser realizado para uma data e horário futuros.");
        }
        if (bloqueioRepository.existsByData(data)) {
            throw new RegraNegocioException("A data selecionada encontra-se bloqueada na agenda (folga ou feriado).");
        }

        DisponibilidadeSemanal disp = disponibilidadeRepository.findByDiaSemana(data.getDayOfWeek())
                .filter(DisponibilidadeSemanal::isAtivo)
                .orElseThrow(() -> new RegraNegocioException("Não há expediente configurado para o dia da semana selecionado."));

        LocalTime horaFim = horaInicio.plusMinutes(disp.getDuracaoSlotMinutos());
        if (horaInicio.isBefore(disp.getHoraInicio()) || horaFim.isAfter(disp.getHoraFim())) {
            throw new RegraNegocioException("O horário escolhido está fora da janela de atendimento ("
                    + disp.getHoraInicio() + " às " + disp.getHoraFim() + ").");
        }

        if (agendamentoRepository.existeConflitoNoHorario(data, horaInicio, STATUS_QUE_OCUPAM_AGENDA, ignorarAgendamentoId)) {
            throw new ConflitoHorarioException("O horário selecionado já está ocupado por outro atendimento.");
        }
        return horaFim;
    }

    public void validarAntecedenciaAutoatendimento(Agendamento agendamento) {
        LocalDateTime agora = LocalDateTime.now(clock);
        Duration antecedencia = Duration.between(agora, agendamento.getDataHoraInicio());
        if (antecedencia.toMinutes() < ANTECEDENCIA_MINIMA_MINUTOS) {
            throw new RegraNegocioException(
                    "Alterações ou cancelamentos pelo autoatendimento exigem no mínimo 2 horas de antecedência. " +
                    "Por favor, fale com o proprietário pelo chat.");
        }
    }

    public void validarTransicaoStatus(Agendamento agendamento, StatusAgendamento novoStatus) {
        if (!agendamento.getStatus().podeTransitarPara(novoStatus)) {
            throw new RegraNegocioException(String.format(
                    "Transição inválida de status: não é permitido alterar de %s para %s.",
                    agendamento.getStatus().getDescricao(),
                    novoStatus != null ? novoStatus.getDescricao() : "NULO"
            ));
        }
    }
}

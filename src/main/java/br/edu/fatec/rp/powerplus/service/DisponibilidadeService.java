package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.Agendamento;
import br.edu.fatec.rp.powerplus.domain.BloqueioAgenda;
import br.edu.fatec.rp.powerplus.domain.DisponibilidadeSemanal;
import br.edu.fatec.rp.powerplus.domain.exception.RecursoNaoEncontradoException;
import br.edu.fatec.rp.powerplus.domain.exception.RegraNegocioException;
import br.edu.fatec.rp.powerplus.repository.AgendamentoRepository;
import br.edu.fatec.rp.powerplus.repository.BloqueioAgendaRepository;
import br.edu.fatec.rp.powerplus.repository.DisponibilidadeSemanalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DisponibilidadeService {

    public record ResultadoBloqueio(BloqueioAgenda bloqueio, int agendamentosConflitantes, List<String> protocolosImpactados) {}

    private final DisponibilidadeSemanalRepository disponibilidadeRepository;
    private final BloqueioAgendaRepository bloqueioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final CalculadoraHorariosLivres calculadoraHorariosLivres;
    private final Clock clock;

    public DisponibilidadeService(DisponibilidadeSemanalRepository disponibilidadeRepository,
                                  BloqueioAgendaRepository bloqueioRepository,
                                  AgendamentoRepository agendamentoRepository,
                                  CalculadoraHorariosLivres calculadoraHorariosLivres,
                                  Clock clock) {
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.calculadoraHorariosLivres = calculadoraHorariosLivres;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<LocalTime> consultarHorariosLivres(LocalDate data) {
        if (data == null) {
            throw new RegraNegocioException("Informe a data para consultar os horários livres.");
        }
        boolean bloqueada = bloqueioRepository.existsByData(data);
        DisponibilidadeSemanal disp = disponibilidadeRepository.findByDiaSemana(data.getDayOfWeek()).orElse(null);
        Set<LocalTime> ocupados = agendamentoRepository
                .listarAtivosNaData(data, PoliticaAgendamentoValidator.STATUS_QUE_OCUPAM_AGENDA)
                .stream()
                .map(Agendamento::getHoraInicio)
                .collect(Collectors.toSet());

        return calculadoraHorariosLivres.calcularSlotsLivres(
                data, disp, bloqueada, ocupados, LocalDateTime.now(clock)
        );
    }

    @Transactional
    public DisponibilidadeSemanal salvarDisponibilidadeDia(DayOfWeek diaSemana, boolean ativo,
                                                           LocalTime horaInicio, LocalTime horaFim,
                                                           int duracaoSlotMinutos) {
        if (diaSemana == null || horaInicio == null || horaFim == null) {
            throw new RegraNegocioException("Dia da semana, horário inicial e horário final são obrigatórios.");
        }
        if (!horaFim.isAfter(horaInicio)) {
            throw new RegraNegocioException("O horário final deve ser posterior ao horário inicial.");
        }
        if (duracaoSlotMinutos < 30 || duracaoSlotMinutos > 240) {
            throw new RegraNegocioException("A duração do slot deve estar entre 30 e 240 minutos.");
        }

        DisponibilidadeSemanal disp = disponibilidadeRepository.findByDiaSemana(diaSemana)
                .orElseGet(() -> new DisponibilidadeSemanal(diaSemana, ativo, horaInicio, horaFim, duracaoSlotMinutos));
        disp.setAtivo(ativo);
        disp.setHoraInicio(horaInicio);
        disp.setHoraFim(horaFim);
        disp.setDuracaoSlotMinutos(duracaoSlotMinutos);
        return disponibilidadeRepository.save(disp);
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadeSemanal> listarDisponibilidadeSemanal() {
        return disponibilidadeRepository.findAllByOrderByDiaSemanaAsc();
    }

    /**
     * UC04 Fluxo Alternativo 3a / RN06: alerta quando já existe cliente marcado na data bloqueada.
     */
    @Transactional
    public ResultadoBloqueio registrarBloqueioData(LocalDate data, String motivo, boolean forcarComConflito) {
        if (data == null || motivo == null || motivo.isBlank()) {
            throw new RegraNegocioException("Data e motivo do bloqueio são obrigatórios.");
        }
        if (bloqueioRepository.existsByData(data)) {
            throw new RegraNegocioException("Esta data já se encontra bloqueada na agenda.");
        }

        List<Agendamento> existentes = agendamentoRepository.listarAtivosNaData(
                data, PoliticaAgendamentoValidator.STATUS_QUE_OCUPAM_AGENDA
        );
        List<String> protocolos = existentes.stream().map(Agendamento::getProtocolo).toList();

        if (!existentes.isEmpty() && !forcarComConflito) {
            throw new RegraNegocioException(String.format(
                    "Já existem %d atendimento(s) confirmado(s) nesta data (%s). Confirme o bloqueio para registrar e remarcar os clientes.",
                    existentes.size(), String.join(", ", protocolos)
            ));
        }

        BloqueioAgenda salvo = bloqueioRepository.save(new BloqueioAgenda(data, motivo.trim()));
        return new ResultadoBloqueio(salvo, existentes.size(), protocolos);
    }

    @Transactional
    public void removerBloqueio(Long bloqueioId) {
        if (!bloqueioRepository.existsById(bloqueioId)) {
            throw new RecursoNaoEncontradoException("Bloqueio não encontrado: " + bloqueioId);
        }
        bloqueioRepository.deleteById(bloqueioId);
    }

    @Transactional(readOnly = true)
    public List<BloqueioAgenda> listarBloqueios() {
        return bloqueioRepository.findAllByOrderByDataAsc();
    }
}

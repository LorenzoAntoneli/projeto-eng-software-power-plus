package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.Agendamento;
import br.edu.fatec.rp.powerplus.domain.BloqueioAgenda;
import br.edu.fatec.rp.powerplus.domain.DisponibilidadeSemanal;
import br.edu.fatec.rp.powerplus.domain.RegraNegocioException;
import br.edu.fatec.rp.powerplus.repository.AgendamentoRepository;
import br.edu.fatec.rp.powerplus.repository.BloqueioAgendaRepository;
import br.edu.fatec.rp.powerplus.repository.DisponibilidadeSemanalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calcula horários livres na agenda, configura o expediente semanal e gerencia bloqueios (folgas/feriados).
 */
@Service
public class DisponibilidadeService {

    public record ResultadoBloqueio(BloqueioAgenda bloqueio, int agendamentosConflitantes, List<String> protocolosImpactados) {}

    private final DisponibilidadeSemanalRepository disponibilidadeRepository;
    private final BloqueioAgendaRepository bloqueioRepository;
    private final AgendamentoRepository agendamentoRepository;

    public DisponibilidadeService(DisponibilidadeSemanalRepository disponibilidadeRepository,
                                  BloqueioAgendaRepository bloqueioRepository,
                                  AgendamentoRepository agendamentoRepository) {
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    @Transactional(readOnly = true)
    public List<LocalTime> consultarHorariosLivres(LocalDate data) {
        if (data == null) {
            throw new RegraNegocioException("Informe a data para consultar os horários livres.");
        }
        LocalDateTime agora = LocalDateTime.now();
        if (data.isBefore(agora.toLocalDate()) || bloqueioRepository.existsByData(data)) {
            return List.of();
        }

        DisponibilidadeSemanal disp = disponibilidadeRepository.findByDiaSemana(data.getDayOfWeek()).orElse(null);
        if (disp == null || !disp.isAtivo()) {
            return List.of();
        }

        Set<LocalTime> horariosOcupados = agendamentoRepository.findByDataOrderByHoraInicioAsc(data).stream()
                .filter(a -> a.getStatus().ocupaHorarioNaAgenda())
                .map(Agendamento::getHoraInicio)
                .collect(Collectors.toSet());

        int passo = Math.max(30, disp.getDuracaoSlotMinutos());
        List<LocalTime> livres = new ArrayList<>();
        LocalTime cursor = disp.getHoraInicio();

        while (!cursor.plusMinutes(passo).isAfter(disp.getHoraFim())) {
            if (LocalDateTime.of(data, cursor).isAfter(agora) && !horariosOcupados.contains(cursor)) {
                livres.add(cursor);
            }
            cursor = cursor.plusMinutes(passo);
        }
        return livres;
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

    @Transactional
    public ResultadoBloqueio registrarBloqueioData(LocalDate data, String motivo, boolean forcarComConflito) {
        if (data == null || motivo == null || motivo.isBlank()) {
            throw new RegraNegocioException("Data e motivo do bloqueio são obrigatórios.");
        }
        if (bloqueioRepository.existsByData(data)) {
            throw new RegraNegocioException("Esta data já se encontra bloqueada na agenda.");
        }

        List<String> protocolosConflitantes = agendamentoRepository.findByDataOrderByHoraInicioAsc(data).stream()
                .filter(a -> a.getStatus().ocupaHorarioNaAgenda())
                .map(Agendamento::getProtocolo)
                .toList();

        if (!protocolosConflitantes.isEmpty() && !forcarComConflito) {
            throw new RegraNegocioException(String.format(
                    "Já existem %d atendimento(s) confirmado(s) nesta data (%s). Confirme o bloqueio para registrar e remarcar os clientes.",
                    protocolosConflitantes.size(), String.join(", ", protocolosConflitantes)
            ));
        }

        BloqueioAgenda salvo = bloqueioRepository.save(new BloqueioAgenda(data, motivo.trim()));
        return new ResultadoBloqueio(salvo, protocolosConflitantes.size(), protocolosConflitantes);
    }

    @Transactional
    public void removerBloqueio(Long bloqueioId) {
        if (!bloqueioRepository.existsById(bloqueioId)) {
            throw RegraNegocioException.naoEncontrado("Bloqueio não encontrado: " + bloqueioId);
        }
        bloqueioRepository.deleteById(bloqueioId);
    }

    @Transactional(readOnly = true)
    public List<BloqueioAgenda> listarBloqueios() {
        return bloqueioRepository.findAllByOrderByDataAsc();
    }
}

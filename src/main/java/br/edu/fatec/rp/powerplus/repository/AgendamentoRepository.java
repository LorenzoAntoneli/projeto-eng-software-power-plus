package br.edu.fatec.rp.powerplus.repository;

import br.edu.fatec.rp.powerplus.domain.Agendamento;
import br.edu.fatec.rp.powerplus.domain.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    Optional<Agendamento> findByProtocoloIgnoreCase(String protocolo);

    List<Agendamento> findAllByOrderByDataAscHoraInicioAsc();

    List<Agendamento> findByDataOrderByHoraInicioAsc(LocalDate data);

    List<Agendamento> findByDataBetweenOrderByDataAscHoraInicioAsc(LocalDate inicio, LocalDate fim);

    List<Agendamento> findByClienteIdOrderByDataDescHoraInicioDesc(Long clienteId);

    List<Agendamento> findByClienteTelefoneOrderByDataDescHoraInicioDesc(String telefoneNormalizado);

    List<Agendamento> findByStatusOrderByDataAscHoraInicioAsc(StatusAgendamento status);

    long countByStatus(StatusAgendamento status);

    @Query("SELECT COUNT(a) > 0 FROM Agendamento a WHERE a.data = :data AND a.horaInicio = :horaInicio " +
           "AND a.status IN :statusAtivos AND (:ignorarId IS NULL OR a.id <> :ignorarId)")
    boolean existeConflitoNoHorario(@Param("data") LocalDate data,
                                    @Param("horaInicio") LocalTime horaInicio,
                                    @Param("statusAtivos") Collection<StatusAgendamento> statusAtivos,
                                    @Param("ignorarId") Long ignorarId);

    @Query("SELECT a FROM Agendamento a WHERE a.data = :data AND a.status IN :statusAtivos ORDER BY a.horaInicio ASC")
    List<Agendamento> listarAtivosNaData(@Param("data") LocalDate data,
                                         @Param("statusAtivos") Collection<StatusAgendamento> statusAtivos);
}

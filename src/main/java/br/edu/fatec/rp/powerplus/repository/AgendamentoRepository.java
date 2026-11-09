package br.edu.fatec.rp.powerplus.repository;

import br.edu.fatec.rp.powerplus.domain.Agendamento;
import br.edu.fatec.rp.powerplus.domain.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
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
}

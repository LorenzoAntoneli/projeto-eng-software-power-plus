package br.edu.fatec.rp.powerplus.repository;

import br.edu.fatec.rp.powerplus.domain.HistoricoStatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatusAgendamento, Long> {
    List<HistoricoStatusAgendamento> findByAgendamentoIdOrderByDataHoraAsc(Long agendamentoId);
}

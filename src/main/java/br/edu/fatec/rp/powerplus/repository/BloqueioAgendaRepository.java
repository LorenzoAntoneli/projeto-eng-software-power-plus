package br.edu.fatec.rp.powerplus.repository;

import br.edu.fatec.rp.powerplus.domain.BloqueioAgenda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BloqueioAgendaRepository extends JpaRepository<BloqueioAgenda, Long> {
    boolean existsByData(LocalDate data);
    Optional<BloqueioAgenda> findByData(LocalDate data);
    List<BloqueioAgenda> findAllByOrderByDataAsc();
}

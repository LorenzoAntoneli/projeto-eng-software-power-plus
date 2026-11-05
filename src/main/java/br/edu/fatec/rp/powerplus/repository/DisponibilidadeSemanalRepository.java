package br.edu.fatec.rp.powerplus.repository;

import br.edu.fatec.rp.powerplus.domain.DisponibilidadeSemanal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface DisponibilidadeSemanalRepository extends JpaRepository<DisponibilidadeSemanal, Long> {
    Optional<DisponibilidadeSemanal> findByDiaSemana(DayOfWeek diaSemana);
    List<DisponibilidadeSemanal> findAllByOrderByDiaSemanaAsc();
}

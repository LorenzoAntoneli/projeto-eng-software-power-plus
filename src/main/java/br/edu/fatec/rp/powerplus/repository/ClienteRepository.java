package br.edu.fatec.rp.powerplus.repository;

import br.edu.fatec.rp.powerplus.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByTelefone(String telefone);

    boolean existsByTelefone(String telefone);

    List<Cliente> findAllByOrderByNomeAsc();
}

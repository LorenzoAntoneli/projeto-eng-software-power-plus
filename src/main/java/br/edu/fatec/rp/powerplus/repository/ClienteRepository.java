package br.edu.fatec.rp.powerplus.repository;

import br.edu.fatec.rp.powerplus.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByTelefone(String telefone);

    boolean existsByTelefone(String telefone);

    @Query("SELECT c FROM Cliente c WHERE LOWER(c.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "OR c.telefone LIKE CONCAT('%', :termoNumerico, '%') ORDER BY c.nome ASC")
    List<Cliente> pesquisarPorNomeOuTelefone(@Param("termo") String termo,
                                             @Param("termoNumerico") String termoNumerico);

    List<Cliente> findAllByOrderByNomeAsc();
}

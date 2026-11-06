package br.edu.fatec.rp.powerplus.repository;

import br.edu.fatec.rp.powerplus.domain.UsuarioProprietario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioProprietarioRepository extends JpaRepository<UsuarioProprietario, Long> {
    Optional<UsuarioProprietario> findByUsernameIgnoreCase(String username);
}

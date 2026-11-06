package br.edu.fatec.rp.powerplus.repository;

import br.edu.fatec.rp.powerplus.domain.NotificacaoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRegistroRepository extends JpaRepository<NotificacaoRegistro, Long> {
    List<NotificacaoRegistro> findTop50ByOrderByCriadaEmDesc();
    List<NotificacaoRegistro> findByProtocoloReferenciaIgnoreCaseOrderByCriadaEmDesc(String protocolo);
    long countByDestinatarioPapelAndLidaFalse(String destinatarioPapel);
}

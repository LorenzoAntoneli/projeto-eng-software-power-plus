package br.edu.fatec.rp.powerplus.repository;

import br.edu.fatec.rp.powerplus.domain.ConversaChat;
import br.edu.fatec.rp.powerplus.domain.StatusConversaChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ConversaChatRepository extends JpaRepository<ConversaChat, Long> {
    List<ConversaChat> findAllByOrderByAtualizadaEmDesc();
    List<ConversaChat> findByStatusInOrderByAtualizadaEmDesc(Collection<StatusConversaChat> status);
    long countByStatus(StatusConversaChat status);
}

package br.edu.fatec.rp.powerplus.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensagens_chat")
public class MensagemChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversa_id", nullable = false)
    private ConversaChat conversa;

    @Column(nullable = false, length = 30)
    private String remetente; // CLIENTE, ASSISTENTE, PROPRIETARIO

    @Column(nullable = false, length = 1000)
    private String conteudo;

    @Column(nullable = false)
    private LocalDateTime enviadaEm;

    public MensagemChat() {
    }

    public MensagemChat(ConversaChat conversa, String remetente, String conteudo, LocalDateTime enviadaEm) {
        this.conversa = conversa;
        this.remetente = remetente;
        this.conteudo = conteudo;
        this.enviadaEm = enviadaEm;
    }

    public Long getId() {
        return id;
    }

    public String getRemetente() {
        return remetente;
    }

    public String getConteudo() {
        return conteudo;
    }

    public LocalDateTime getEnviadaEm() {
        return enviadaEm;
    }
}

package br.edu.fatec.rp.powerplus.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversas_chat")
public class ConversaChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nomeCliente;

    @Column(length = 20)
    private String telefoneCliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusConversaChat status = StatusConversaChat.AUTOMATICO;

    @OneToMany(mappedBy = "conversa", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("enviadaEm ASC, id ASC")
    private List<MensagemChat> mensagens = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime atualizadaEm = LocalDateTime.now();

    public ConversaChat() {
    }

    public ConversaChat(String nomeCliente, String telefoneCliente, LocalDateTime agora) {
        this.nomeCliente = nomeCliente;
        this.telefoneCliente = Cliente.normalizarTelefone(telefoneCliente);
        this.status = StatusConversaChat.AUTOMATICO;
        this.criadaEm = agora;
        this.atualizadaEm = agora;
    }

    public void adicionarMensagem(String remetente, String conteudo, LocalDateTime instante) {
        MensagemChat msg = new MensagemChat(this, remetente, conteudo, instante);
        this.mensagens.add(msg);
        this.atualizadaEm = instante;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public String getTelefoneCliente() {
        return telefoneCliente;
    }

    public StatusConversaChat getStatus() {
        return status;
    }

    public void setStatus(StatusConversaChat status) {
        this.status = status;
    }

    public List<MensagemChat> getMensagens() {
        return mensagens;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDateTime getAtualizadaEm() {
        return atualizadaEm;
    }

    public void setAtualizadaEm(LocalDateTime atualizadaEm) {
        this.atualizadaEm = atualizadaEm;
    }
}

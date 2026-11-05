package br.edu.fatec.rp.powerplus.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacoes_registro")
public class NotificacaoRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacao tipo;

    @Column(nullable = false, length = 30)
    private String destinatarioPapel;

    @Column(nullable = false, length = 140)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String mensagem;

    @Column(length = 30)
    private String protocoloReferencia;

    @Column(nullable = false, length = 40)
    private String canalEnvio = "SISTEMA_INTERNO";

    @Column(nullable = false)
    private boolean lida = false;

    @Column(nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();

    public NotificacaoRegistro() {
    }

    public NotificacaoRegistro(TipoNotificacao tipo, String destinatarioPapel, String titulo,
                               String mensagem, String protocoloReferencia, String canalEnvio,
                               LocalDateTime criadaEm) {
        this.tipo = tipo;
        this.destinatarioPapel = destinatarioPapel;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.protocoloReferencia = protocoloReferencia;
        this.canalEnvio = canalEnvio;
        this.criadaEm = criadaEm;
    }

    public Long getId() {
        return id;
    }

    public TipoNotificacao getTipo() {
        return tipo;
    }

    public String getDestinatarioPapel() {
        return destinatarioPapel;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public String getProtocoloReferencia() {
        return protocoloReferencia;
    }

    public String getCanalEnvio() {
        return canalEnvio;
    }

    public boolean isLida() {
        return lida;
    }

    public void setLida(boolean lida) {
        this.lida = lida;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }
}

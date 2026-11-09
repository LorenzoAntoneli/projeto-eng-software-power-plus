package br.edu.fatec.rp.powerplus.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "clientes", indexes = {
        @Index(name = "idx_cliente_telefone", columnList = "telefone", unique = true),
        @Index(name = "idx_cliente_nome", columnList = "nome")
})
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 20)
    private String telefone;

    @Column(length = 120)
    private String email;

    @Column(nullable = false, length = 220)
    private String enderecoPadrao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PerfilCliente perfil = PerfilCliente.RESIDENCIAL;

    @Column(length = 400)
    private String observacoes;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public Cliente() {
    }

    public Cliente(String nome, String telefone, String enderecoPadrao, PerfilCliente perfil) {
        this.nome = nome;
        this.telefone = normalizarTelefone(telefone);
        this.enderecoPadrao = enderecoPadrao;
        this.perfil = perfil != null ? perfil : PerfilCliente.RESIDENCIAL;
        this.criadoEm = LocalDateTime.now();
    }

    public static String normalizarTelefone(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = normalizarTelefone(telefone); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEnderecoPadrao() { return enderecoPadrao; }
    public void setEnderecoPadrao(String enderecoPadrao) { this.enderecoPadrao = enderecoPadrao; }
    public PerfilCliente getPerfil() { return perfil; }
    public void setPerfil(PerfilCliente perfil) { this.perfil = perfil; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}

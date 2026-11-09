package br.edu.fatec.rp.powerplus.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios_proprietario")
public class UsuarioProprietario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(nullable = false, length = 120)
    private String nomeExibicao;

    @Column(nullable = false, length = 120)
    private String senhaHash;

    public UsuarioProprietario() {
    }

    public UsuarioProprietario(String username, String nomeExibicao, String senhaHash) {
        this.username = username;
        this.nomeExibicao = nomeExibicao;
        this.senhaHash = senhaHash;
    }

    public String getUsername() { return username; }
    public String getNomeExibicao() { return nomeExibicao; }
    public String getSenhaHash() { return senhaHash; }
}

package br.edu.fatec.rp.powerplus.domain;

public enum PerfilCliente {
    RESIDENCIAL("Residencial"),
    EMPRESARIAL_CONDOMINIO("Empresarial / Condomínio");

    private final String descricao;

    PerfilCliente(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}

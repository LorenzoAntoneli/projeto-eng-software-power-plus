package br.edu.fatec.rp.powerplus.domain;

public enum TipoServico {
    REPARO_ELETRICO("Reparo Elétrico / Curto-Circuito", 60),
    INSTALACAO_ELETRICA("Instalação de Tomadas, Iluminação ou Chuveiro", 60),
    QUADRO_DISJUNTORES("Manutenção / Montagem de Quadro de Disjuntores", 60),
    MANUTENCAO_PREVENTIVA("Manutenção Preventiva (Empresa / Condomínio)", 60),
    VISTORIA_ORCAMENTO("Vistoria Técnica e Orçamento Presencial", 60);

    private final String descricao;
    private final int duracaoPadraoMinutos;

    TipoServico(String descricao, int duracaoPadraoMinutos) {
        this.descricao = descricao;
        this.duracaoPadraoMinutos = duracaoPadraoMinutos;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getDuracaoPadraoMinutos() {
        return duracaoPadraoMinutos;
    }
}

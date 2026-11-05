package br.edu.fatec.rp.powerplus.domain;

public enum StatusConversaChat {
    AUTOMATICO("Atendimento Automático"),
    AGUARDANDO_HUMANO("Aguardando Proprietário"),
    EM_ATENDIMENTO_HUMANO("Assumido pelo Proprietário"),
    ENCERRADO("Encerrado");

    private final String descricao;

    StatusConversaChat(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}

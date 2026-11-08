package br.edu.fatec.rp.powerplus.domain;

public enum StatusAgendamento {
    AGUARDANDO_APROVACAO("Aguardando aprovação"),
    CONFIRMADO("Confirmado"),
    EM_ATENDIMENTO("Em Atendimento"),
    CONCLUIDO("Concluído"),
    RECUSADO("Recusado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusAgendamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean podeTransitarPara(StatusAgendamento novoStatus) {
        if (novoStatus == null || this == novoStatus) {
            return false;
        }
        return switch (this) {
            case AGUARDANDO_APROVACAO -> novoStatus == CONFIRMADO
                    || novoStatus == RECUSADO
                    || novoStatus == CANCELADO;
            case CONFIRMADO -> novoStatus == EM_ATENDIMENTO
                    || novoStatus == CONCLUIDO
                    || novoStatus == CANCELADO;
            case EM_ATENDIMENTO -> novoStatus == CONCLUIDO
                    || novoStatus == CANCELADO;
            case CONCLUIDO, RECUSADO, CANCELADO -> false;
        };
    }

    public boolean ocupaHorarioNaAgenda() {
        return this == AGUARDANDO_APROVACAO || this == CONFIRMADO || this == EM_ATENDIMENTO;
    }
}

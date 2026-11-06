package br.edu.fatec.rp.powerplus.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_status_agendamento")
public class HistoricoStatusAgendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamento_id", nullable = false)
    private Agendamento agendamento;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private StatusAgendamento statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatusAgendamento statusNovo;

    @Column(nullable = false, length = 80)
    private String responsavel;

    @Column(length = 250)
    private String observacao;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    public HistoricoStatusAgendamento() {
    }

    public HistoricoStatusAgendamento(Agendamento agendamento, StatusAgendamento statusAnterior,
                                      StatusAgendamento statusNovo, String responsavel,
                                      String observacao, LocalDateTime dataHora) {
        this.agendamento = agendamento;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.responsavel = responsavel;
        this.observacao = observacao;
        this.dataHora = dataHora;
    }

    public Long getId() {
        return id;
    }

    public Agendamento getAgendamento() {
        return agendamento;
    }

    public StatusAgendamento getStatusAnterior() {
        return statusAnterior;
    }

    public StatusAgendamento getStatusNovo() {
        return statusNovo;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public String getObservacao() {
        return observacao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}

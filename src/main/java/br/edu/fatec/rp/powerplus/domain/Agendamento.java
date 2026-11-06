package br.edu.fatec.rp.powerplus.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "agendamentos", indexes = {
        @Index(name = "idx_agendamento_protocolo", columnList = "protocolo", unique = true),
        @Index(name = "idx_agendamento_data_hora", columnList = "data,horaInicio")
})
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 24)
    private String protocolo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 35)
    private TipoServico tipoServico;

    @Column(nullable = false, length = 220)
    private String enderecoAtendimento;

    @Column(length = 500)
    private String descricaoServico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatusAgendamento status = StatusAgendamento.CONFIRMADO;

    @Column(length = 400)
    private String observacaoTecnica;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public Agendamento() {
    }

    public Agendamento(String protocolo, Cliente cliente, LocalDate data, LocalTime horaInicio,
                       LocalTime horaFim, TipoServico tipoServico,
                       String enderecoAtendimento, String descricaoServico) {
        this.protocolo = protocolo;
        this.cliente = cliente;
        this.data = data;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.tipoServico = tipoServico;
        this.enderecoAtendimento = enderecoAtendimento;
        this.descricaoServico = descricaoServico;
        this.status = StatusAgendamento.CONFIRMADO;
    }

    public LocalDateTime getDataHoraInicio() {
        return LocalDateTime.of(data, horaInicio);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProtocolo() {
        return protocolo;
    }

    public void setProtocolo(String protocolo) {
        this.protocolo = protocolo;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }

    public void setHoraFim(LocalTime horaFim) {
        this.horaFim = horaFim;
    }

    public TipoServico getTipoServico() {
        return tipoServico;
    }

    public void setTipoServico(TipoServico tipoServico) {
        this.tipoServico = tipoServico;
    }

    public String getEnderecoAtendimento() {
        return enderecoAtendimento;
    }

    public void setEnderecoAtendimento(String enderecoAtendimento) {
        this.enderecoAtendimento = enderecoAtendimento;
    }

    public String getDescricaoServico() {
        return descricaoServico;
    }

    public void setDescricaoServico(String descricaoServico) {
        this.descricaoServico = descricaoServico;
    }

    public StatusAgendamento getStatus() {
        return status;
    }

    public void setStatus(StatusAgendamento status) {
        this.status = status;
    }

    public String getObservacaoTecnica() {
        return observacaoTecnica;
    }

    public void setObservacaoTecnica(String observacaoTecnica) {
        this.observacaoTecnica = observacaoTecnica;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}

package br.edu.fatec.rp.powerplus.domain;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "disponibilidades_semanais", indexes = {
        @Index(name = "idx_disp_dia_semana", columnList = "diaSemana", unique = true)
})
public class DisponibilidadeSemanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private DayOfWeek diaSemana;

    @Column(nullable = false)
    private boolean ativo;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFim;

    @Column(nullable = false)
    private int duracaoSlotMinutos = 60;

    public DisponibilidadeSemanal() {
    }

    public DisponibilidadeSemanal(DayOfWeek diaSemana, boolean ativo,
                                  LocalTime horaInicio, LocalTime horaFim, int duracaoSlotMinutos) {
        this.diaSemana = diaSemana;
        this.ativo = ativo;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.duracaoSlotMinutos = duracaoSlotMinutos;
    }

    public Long getId() { return id; }
    public DayOfWeek getDiaSemana() { return diaSemana; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }
    public int getDuracaoSlotMinutos() { return duracaoSlotMinutos; }
    public void setDuracaoSlotMinutos(int duracaoSlotMinutos) { this.duracaoSlotMinutos = duracaoSlotMinutos; }
}

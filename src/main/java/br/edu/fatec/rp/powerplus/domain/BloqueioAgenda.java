package br.edu.fatec.rp.powerplus.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bloqueios_agenda", indexes = {
        @Index(name = "idx_bloqueio_data", columnList = "data", unique = true)
})
public class BloqueioAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate data;

    @Column(nullable = false, length = 180)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public BloqueioAgenda() {
    }

    public BloqueioAgenda(LocalDate data, String motivo) {
        this.data = data;
        this.motivo = motivo;
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public LocalDate getData() { return data; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}

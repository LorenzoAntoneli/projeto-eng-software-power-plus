package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.DisponibilidadeSemanal;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Componente puro para geração dos slots livres de agendamento (HU01/HU07/HU08).
 */
@Component
public class CalculadoraHorariosLivres {

    public List<LocalTime> calcularSlotsLivres(LocalDate dataAlvo,
                                               DisponibilidadeSemanal disponibilidade,
                                               boolean dataBloqueada,
                                               Set<LocalTime> horariosOcupados,
                                               LocalDateTime instanteAtual) {
        if (dataBloqueada || disponibilidade == null || !disponibilidade.isAtivo()) {
            return List.of();
        }
        if (dataAlvo.isBefore(instanteAtual.toLocalDate())) {
            return List.of();
        }

        int passo = Math.max(30, disponibilidade.getDuracaoSlotMinutos());
        List<LocalTime> livres = new ArrayList<>();
        LocalTime cursor = disponibilidade.getHoraInicio();

        while (!cursor.plusMinutes(passo).isAfter(disponibilidade.getHoraFim())) {
            boolean ehFuturo = LocalDateTime.of(dataAlvo, cursor).isAfter(instanteAtual);
            boolean livre = !horariosOcupados.contains(cursor);
            if (ehFuturo && livre) {
                livres.add(cursor);
            }
            cursor = cursor.plusMinutes(passo);
        }
        return livres;
    }
}

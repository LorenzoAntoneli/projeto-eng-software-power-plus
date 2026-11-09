package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.*;
import br.edu.fatec.rp.powerplus.repository.AgendamentoRepository;
import br.edu.fatec.rp.powerplus.repository.BloqueioAgendaRepository;
import br.edu.fatec.rp.powerplus.repository.DisponibilidadeSemanalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisponibilidadeServiceTest {

    @Mock private DisponibilidadeSemanalRepository disponibilidadeRepository;
    @Mock private BloqueioAgendaRepository bloqueioRepository;
    @Mock private AgendamentoRepository agendamentoRepository;
    @InjectMocks private DisponibilidadeService disponibilidadeService;

    @Test
    @DisplayName("Lista apenas horários vagos dentro do expediente")
    void deveListarApenasHorariosFuturosELivres() {
        LocalDate proximaQuinta = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.THURSDAY));
        DisponibilidadeSemanal quinta = new DisponibilidadeSemanal(
                DayOfWeek.THURSDAY, true, LocalTime.of(10, 0), LocalTime.of(13, 0), 60
        );
        Cliente cli = new Cliente("Ana", "16991112222", "Rua X", PerfilCliente.RESIDENCIAL);
        Agendamento ocupadoAs11 = new Agendamento(
                "PP-2026-0001", cli, proximaQuinta, LocalTime.of(11, 0), LocalTime.of(12, 0),
                TipoServico.REPARO_ELETRICO, "Rua X", null
        );

        when(bloqueioRepository.existsByData(proximaQuinta)).thenReturn(false);
        when(disponibilidadeRepository.findByDiaSemana(DayOfWeek.THURSDAY)).thenReturn(Optional.of(quinta));
        when(agendamentoRepository.findByDataOrderByHoraInicioAsc(proximaQuinta)).thenReturn(List.of(ocupadoAs11));

        List<LocalTime> livres = disponibilidadeService.consultarHorariosLivres(proximaQuinta);

        assertEquals(List.of(LocalTime.of(10, 0), LocalTime.of(12, 0)), livres);
    }

    @Test
    @DisplayName("Exige confirmação antes de bloquear data que já possui agendamento ativo")
    void deveAlertarAoBloquearDataComAgendamentoAtivo() {
        LocalDate data = LocalDate.now().plusDays(5);
        Cliente cli = new Cliente("Condomínio Bela Vista", "1636112233", "Av. Brasil, 90", PerfilCliente.EMPRESARIAL_CONDOMINIO);
        Agendamento existente = new Agendamento(
                "PP-2026-0042", cli, data, LocalTime.of(14, 0), LocalTime.of(15, 0),
                TipoServico.MANUTENCAO_PREVENTIVA, "Av. Brasil, 90", null
        );

        when(bloqueioRepository.existsByData(data)).thenReturn(false);
        when(agendamentoRepository.findByDataOrderByHoraInicioAsc(data)).thenReturn(List.of(existente));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> disponibilidadeService.registrarBloqueioData(data, "Compromisso médico", false));

        assertTrue(ex.getMessage().contains("PP-2026-0042"));
        verify(bloqueioRepository, never()).save(any());

        when(bloqueioRepository.save(any(BloqueioAgenda.class))).thenAnswer(inv -> inv.getArgument(0));
        DisponibilidadeService.ResultadoBloqueio res = disponibilidadeService.registrarBloqueioData(data, "Compromisso médico", true);
        assertEquals(1, res.agendamentosConflitantes());
        assertEquals(List.of("PP-2026-0042"), res.protocolosImpactados());
    }
}

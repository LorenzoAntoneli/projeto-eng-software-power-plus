package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.*;
import br.edu.fatec.rp.powerplus.domain.exception.RegraNegocioException;
import br.edu.fatec.rp.powerplus.repository.AgendamentoRepository;
import br.edu.fatec.rp.powerplus.repository.BloqueioAgendaRepository;
import br.edu.fatec.rp.powerplus.repository.DisponibilidadeSemanalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisponibilidadeServiceTest {

    @Mock
    private DisponibilidadeSemanalRepository disponibilidadeRepository;
    @Mock
    private BloqueioAgendaRepository bloqueioRepository;
    @Mock
    private AgendamentoRepository agendamentoRepository;

    private DisponibilidadeService disponibilidadeService;

    @BeforeEach
    void setUp() {
        ZoneId zona = ZoneId.of("America/Sao_Paulo");
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 9, 10, 9, 30).atZone(zona).toInstant(), zona);
        disponibilidadeService = new DisponibilidadeService(
                disponibilidadeRepository,
                bloqueioRepository,
                agendamentoRepository,
                new CalculadoraHorariosLivres(),
                clock
        );
    }

    @Test
    @DisplayName("TU11 - Deve listar apenas horários futuros e desocupados dentro do expediente (HU01/HU07/HU08)")
    void deveListarApenasHorariosFuturosELivres() {
        LocalDate hoje = LocalDate.of(2026, 9, 10); // relógio às 09:30
        DisponibilidadeSemanal quinta = new DisponibilidadeSemanal(
                DayOfWeek.THURSDAY, true, LocalTime.of(8, 0), LocalTime.of(13, 0), 60
        );
        Cliente cli = new Cliente("Ana", "16991112222", "Rua X", PerfilCliente.RESIDENCIAL);
        Agendamento ocupadoAs11 = new Agendamento(
                "PP-2026-0001", cli, hoje, LocalTime.of(11, 0), LocalTime.of(12, 0),
                TipoServico.REPARO_ELETRICO, "Rua X", null
        );

        when(bloqueioRepository.existsByData(hoje)).thenReturn(false);
        when(disponibilidadeRepository.findByDiaSemana(DayOfWeek.THURSDAY)).thenReturn(Optional.of(quinta));
        when(agendamentoRepository.listarAtivosNaData(eq(hoje), anyCollection())).thenReturn(List.of(ocupadoAs11));

        List<LocalTime> livres = disponibilidadeService.consultarHorariosLivres(hoje);

        // 08:00 e 09:00 já passaram (relógio 09:30); 11:00 está ocupado -> sobram 10:00 e 12:00
        assertEquals(List.of(LocalTime.of(10, 0), LocalTime.of(12, 0)), livres);
    }

    @Test
    @DisplayName("TU12 - Deve alertar conflito ao bloquear data que já possui cliente agendado (UC04 3a / RN06)")
    void deveAlertarAoBloquearDataComAgendamentoAtivo() {
        LocalDate data = LocalDate.of(2026, 9, 15);
        Cliente cli = new Cliente("Condomínio Bela Vista", "1636112233", "Av. Brasil, 90", PerfilCliente.EMPRESARIAL_CONDOMINIO);
        Agendamento existente = new Agendamento(
                "PP-2026-0042", cli, data, LocalTime.of(14, 0), LocalTime.of(15, 0),
                TipoServico.MANUTENCAO_PREVENTIVA, "Av. Brasil, 90", null
        );

        when(bloqueioRepository.existsByData(data)).thenReturn(false);
        when(agendamentoRepository.listarAtivosNaData(eq(data), anyCollection())).thenReturn(List.of(existente));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> disponibilidadeService.registrarBloqueioData(data, "Compromisso médico", false));

        assertTrue(ex.getMessage().contains("PP-2026-0042"));
        verify(bloqueioRepository, never()).save(any());

        // Ao confirmar explicitamente (forcarComConflito = true), registra o bloqueio retornando os protocolos impactados
        when(bloqueioRepository.save(any(BloqueioAgenda.class))).thenAnswer(inv -> inv.getArgument(0));
        DisponibilidadeService.ResultadoBloqueio res = disponibilidadeService.registrarBloqueioData(data, "Compromisso médico", true);
        assertEquals(1, res.agendamentosConflitantes());
        assertEquals(List.of("PP-2026-0042"), res.protocolosImpactados());
    }
}

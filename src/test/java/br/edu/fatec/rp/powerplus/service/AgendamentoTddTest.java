package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.*;
import br.edu.fatec.rp.powerplus.domain.exception.ConflitoHorarioException;
import br.edu.fatec.rp.powerplus.domain.exception.RegraNegocioException;
import br.edu.fatec.rp.powerplus.repository.*;
import br.edu.fatec.rp.powerplus.service.notification.NotificacaoGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoTddTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;
    @Mock
    private DisponibilidadeSemanalRepository disponibilidadeRepository;
    @Mock
    private BloqueioAgendaRepository bloqueioRepository;
    @Mock
    private HistoricoStatusRepository historicoRepository;
    @Mock
    private ClienteService clienteService;
    @Mock
    private NotificacaoGateway notificacaoGateway;

    private Clock clockFixo;
    private AgendamentoService agendamentoService;

    @BeforeEach
    void setUp() {
        // Stub determinístico de relógio: Quinta-feira, 10/09/2026 às 09:00 (America/Sao_Paulo)
        ZoneId zona = ZoneId.of("America/Sao_Paulo");
        Instant instanteFixo = LocalDateTime.of(2026, 9, 10, 9, 0).atZone(zona).toInstant();
        clockFixo = Clock.fixed(instanteFixo, zona);

        agendamentoService = new AgendamentoService(
                agendamentoRepository,
                disponibilidadeRepository,
                bloqueioRepository,
                historicoRepository,
                clienteService,
                notificacaoGateway,
                clockFixo
        );
    }

    @Test
    @DisplayName("TU05 [TDD] - Deve impedir dois agendamentos ativos no mesmo horário (HU08/RN01)")
    void deveImpedirDoisAgendamentosNoMesmoHorario() {
        LocalDate sexta = LocalDate.of(2026, 9, 11);
        LocalTime horario = LocalTime.of(14, 0);

        when(bloqueioRepository.existsByData(sexta)).thenReturn(false);
        when(disponibilidadeRepository.findByDiaSemana(DayOfWeek.FRIDAY)).thenReturn(Optional.of(
                new DisponibilidadeSemanal(DayOfWeek.FRIDAY, true, LocalTime.of(8, 0), LocalTime.of(18, 0), 60)
        ));
        when(agendamentoRepository.existeConflitoNoHorario(eq(sexta), eq(horario), anyCollection(), isNull()))
                .thenReturn(true);

        assertThrows(ConflitoHorarioException.class, () -> agendamentoService.agendarAutoatendimento(
                "Roberto Almeida",
                "(16) 99222-3344",
                "roberto@condominio.com",
                "Rua Conde Afonso Celso, 1500",
                PerfilCliente.EMPRESARIAL_CONDOMINIO,
                sexta,
                horario,
                TipoServico.MANUTENCAO_PREVENTIVA,
                "Revisão do quadro de bombas"
        ));

        verify(agendamentoRepository, never()).save(any());
        verify(notificacaoGateway, never()).notificarAgendamentoConfirmado(any());
    }

    @Test
    @DisplayName("TU06 [TDD] - Deve impedir agendamento em data bloqueada ou fora do expediente (HU07/RN02)")
    void deveImpedirAgendamentoEmDataBloqueadaOuForaDoExpediente() {
        LocalDate feriadoBloqueado = LocalDate.of(2026, 9, 14);
        when(bloqueioRepository.existsByData(feriadoBloqueado)).thenReturn(true);

        assertThrows(RegraNegocioException.class, () -> agendamentoService.agendarAutoatendimento(
                "Mariana Costa",
                "(16) 99111-0000",
                null,
                "Av. Nove de Julho, 500",
                PerfilCliente.RESIDENCIAL,
                feriadoBloqueado,
                LocalTime.of(10, 0),
                TipoServico.INSTALACAO_ELETRICA,
                "Instalação de chuveiro 220V"
        ));
    }

    @Test
    @DisplayName("TU07 [TDD] - Deve bloquear cancelamento pelo cliente com menos de 2h de antecedência (HU04/RN03)")
    void deveBloquearCancelamentoPeloClienteComMenosDeDuasHorasDeAntecedencia() {
        Cliente cliente = new Cliente("Mariana Costa", "16991110000", "Rua A, 10", PerfilCliente.RESIDENCIAL);
        // Relógio fixo em 10/09/2026 09:00 -> visita às 10:00 (apenas 1h de antecedência)
        Agendamento agendamentoProximo = new Agendamento(
                "PP-2026-0010",
                cliente,
                LocalDate.of(2026, 9, 10),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                TipoServico.REPARO_ELETRICO,
                "Rua A, 10",
                "Tomada em curto"
        );
        agendamentoProximo.setId(10L);
        when(agendamentoRepository.findByProtocoloIgnoreCase("PP-2026-0010"))
                .thenReturn(Optional.of(agendamentoProximo));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> agendamentoService.cancelarPeloCliente("PP-2026-0010", "Imprevisto"));

        assertTrue(ex.getMessage().toLowerCase().contains("2 horas"));
        assertEquals(StatusAgendamento.CONFIRMADO, agendamentoProximo.getStatus());
        verify(notificacaoGateway, never()).notificarAgendamentoCancelado(any(), anyString());
    }

    @Test
    @DisplayName("TU08 [TDD + Mock] - Deve cancelar dentro do prazo, liberar horário e acionar NotificacaoGateway (HU03/HU04/HU10)")
    void deveLiberarHorarioERegistrarNotificacaoAoCancelarDentroDoPrazo() {
        Cliente cliente = new Cliente("Mariana Costa", "16991110000", "Rua A, 10", PerfilCliente.RESIDENCIAL);
        // Relógio fixo em 10/09/2026 09:00 -> visita às 15:00 (6h de antecedência >= 2h)
        Agendamento agendamento = new Agendamento(
                "PP-2026-0011",
                cliente,
                LocalDate.of(2026, 9, 10),
                LocalTime.of(15, 0),
                LocalTime.of(16, 0),
                TipoServico.INSTALACAO_ELETRICA,
                "Rua A, 10",
                "Instalar luminárias"
        );
        agendamento.setId(11L);

        when(agendamentoRepository.findByProtocoloIgnoreCase("PP-2026-0011")).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Agendamento cancelado = agendamentoService.cancelarPeloCliente("PP-2026-0011", "Viagem de trabalho");

        assertEquals(StatusAgendamento.CANCELADO, cancelado.getStatus());
        assertFalse(cancelado.getStatus().ocupaHorarioNaAgenda());
        verify(historicoRepository).save(any(HistoricoStatusAgendamento.class));
        verify(notificacaoGateway).notificarAgendamentoCancelado(eq(cancelado), contains("CLIENTE"));
    }

    @Test
    @DisplayName("TU09 [TDD - Negativo] - Deve impedir transição inválida de CANCELADO diretamente para CONCLUIDO (RN04)")
    void deveImpedirTransicaoInvalidaDeCanceladoParaConcluido() {
        Cliente cliente = new Cliente("Paulo Silva", "16998887766", "Rua B, 20", PerfilCliente.RESIDENCIAL);
        Agendamento cancelado = new Agendamento(
                "PP-2026-0012",
                cliente,
                LocalDate.of(2026, 9, 12),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                TipoServico.VISTORIA_ORCAMENTO,
                "Rua B, 20",
                "Orçamento padrão bifásico"
        );
        cancelado.setId(12L);
        cancelado.setStatus(StatusAgendamento.CANCELADO);

        when(agendamentoRepository.findById(12L)).thenReturn(Optional.of(cancelado));

        assertThrows(RegraNegocioException.class, () -> agendamentoService.atualizarStatusOperacional(
                12L, StatusAgendamento.CONCLUIDO, "Proprietário", "Tentativa inválida"
        ));

        verify(agendamentoRepository, never()).save(any());
    }
}

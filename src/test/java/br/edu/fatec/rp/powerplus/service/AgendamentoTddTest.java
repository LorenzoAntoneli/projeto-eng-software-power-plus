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
        ZoneId zona = ZoneId.of("America/Sao_Paulo");
        Instant instanteFixo = LocalDateTime.of(2026, 9, 10, 9, 0).atZone(zona).toInstant();
        clockFixo = Clock.fixed(instanteFixo, zona);

        PoliticaAgendamentoValidator validador = new PoliticaAgendamentoValidator(
                agendamentoRepository,
                disponibilidadeRepository,
                bloqueioRepository,
                clockFixo
        );

        agendamentoService = new AgendamentoService(
                agendamentoRepository,
                historicoRepository,
                clienteService,
                validador,
                notificacaoGateway,
                clockFixo
        );
    }

    @Test
    @DisplayName("Impede dois agendamentos ativos no mesmo horário")
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
    @DisplayName("Impede agendamento em data bloqueada ou fora do expediente")
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
    @DisplayName("Bloqueia cancelamento pelo cliente com menos de 2 horas de antecedência")
    void deveBloquearCancelamentoPeloClienteComMenosDeDuasHorasDeAntecedencia() {
        Cliente cliente = new Cliente("Mariana Costa", "16991110000", "Rua A, 10", PerfilCliente.RESIDENCIAL);
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
    @DisplayName("Cancela dentro do prazo, libera horário e gera notificação")
    void deveLiberarHorarioERegistrarNotificacaoAoCancelarDentroDoPrazo() {
        Cliente cliente = new Cliente("Mariana Costa", "16991110000", "Rua A, 10", PerfilCliente.RESIDENCIAL);
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
    @DisplayName("Impede alterar status de cancelado diretamente para concluído")
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

    @Test
    @DisplayName("Remarca agendamento válido e notifica o proprietário")
    void deveRemarcarAgendamentoValidoENotificarProprietario() {
        Cliente cliente = new Cliente("Mariana Costa", "16991110000", "Rua A, 10", PerfilCliente.RESIDENCIAL);
        Agendamento existente = new Agendamento(
                "PP-2026-0015",
                cliente,
                LocalDate.of(2026, 9, 11),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                TipoServico.QUADRO_DISJUNTORES,
                "Rua A, 10",
                "Troca de DR"
        );
        existente.setId(15L);

        LocalDate novaData = LocalDate.of(2026, 9, 11);
        LocalTime novoHorario = LocalTime.of(16, 0);

        when(agendamentoRepository.findByProtocoloIgnoreCase("PP-2026-0015")).thenReturn(Optional.of(existente));
        when(bloqueioRepository.existsByData(novaData)).thenReturn(false);
        when(disponibilidadeRepository.findByDiaSemana(DayOfWeek.FRIDAY)).thenReturn(Optional.of(
                new DisponibilidadeSemanal(DayOfWeek.FRIDAY, true, LocalTime.of(8, 0), LocalTime.of(18, 0), 60)
        ));
        when(agendamentoRepository.existeConflitoNoHorario(eq(novaData), eq(novoHorario), anyCollection(), eq(15L)))
                .thenReturn(false);
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Agendamento remarcado = agendamentoService.remarcarPeloCliente("PP-2026-0015", novaData, novoHorario, "Reunião");

        assertEquals(LocalTime.of(16, 0), remarcado.getHoraInicio());
        assertEquals(LocalTime.of(17, 0), remarcado.getHoraFim());
        verify(notificacaoGateway).notificarAgendamentoRemarcado(eq(remarcado), eq("11/09/2026 10:00"));
    }

    @Test
    @DisplayName("Cria solicitação inicial como aguardando aprovação e reserva o horário")
    void deveCriarSolicitacaoComStatusAguardandoAprovacao() {
        LocalDate sexta = LocalDate.of(2026, 9, 11);
        LocalTime horario = LocalTime.of(14, 0);
        Cliente cli = new Cliente("Mariana Costa", "16991110000", "Rua A, 10", PerfilCliente.RESIDENCIAL);

        when(bloqueioRepository.existsByData(sexta)).thenReturn(false);
        when(disponibilidadeRepository.findByDiaSemana(DayOfWeek.FRIDAY)).thenReturn(Optional.of(
                new DisponibilidadeSemanal(DayOfWeek.FRIDAY, true, LocalTime.of(8, 0), LocalTime.of(18, 0), 60)
        ));
        when(agendamentoRepository.existeConflitoNoHorario(eq(sexta), eq(horario), anyCollection(), isNull()))
                .thenReturn(false);
        when(clienteService.obterOuCadastrarNoAgendamento(anyString(), anyString(), any(), anyString(), any()))
                .thenReturn(cli);
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Agendamento criado = agendamentoService.agendarAutoatendimento(
                "Mariana Costa", "(16) 99111-0000", "mariana@exemplo.com",
                "Rua A, 10", PerfilCliente.RESIDENCIAL, sexta, horario,
                TipoServico.INSTALACAO_ELETRICA, "Instalar chuveiro 220V"
        );

        assertEquals(StatusAgendamento.AGUARDANDO_APROVACAO, criado.getStatus());
        assertEquals("Aguardando aprovação", criado.getStatus().getDescricao());
        assertTrue(criado.getStatus().ocupaHorarioNaAgenda());
        verify(notificacaoGateway).notificarSolicitacaoRecebida(eq(criado));
        verify(notificacaoGateway, never()).notificarAgendamentoConfirmado(any());
    }

    @Test
    @DisplayName("Aprova visita pendente mudando status para confirmado")
    void deveAprovarSolicitacaoPendenteConfirmandoVisitaENotificandoCliente() {
        Cliente cliente = new Cliente("Mariana Costa", "16991110000", "Rua A, 10", PerfilCliente.RESIDENCIAL);
        Agendamento pendente = new Agendamento(
                "PP-2026-0020", cliente, LocalDate.of(2026, 9, 11),
                LocalTime.of(14, 0), LocalTime.of(15, 0),
                TipoServico.INSTALACAO_ELETRICA, "Rua A, 10", "Instalar chuveiro"
        );
        pendente.setId(20L);
        pendente.setStatus(StatusAgendamento.AGUARDANDO_APROVACAO);

        when(agendamentoRepository.findById(20L)).thenReturn(Optional.of(pendente));
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Agendamento aprovado = agendamentoService.aprovarSolicitacao(20L, "Leonardo", "Visita confirmada na rota da tarde");

        assertEquals(StatusAgendamento.CONFIRMADO, aprovado.getStatus());
        assertTrue(aprovado.getStatus().ocupaHorarioNaAgenda());
        verify(historicoRepository).save(any(HistoricoStatusAgendamento.class));
        verify(notificacaoGateway).notificarAgendamentoConfirmado(eq(aprovado));
    }

    @Test
    @DisplayName("Recusa visita pendente liberando a vaga na agenda")
    void deveRecusarSolicitacaoPendenteLiberandoHorarioNaAgenda() {
        Cliente cliente = new Cliente("Roberto Almeida", "16992223344", "Av. B, 500", PerfilCliente.EMPRESARIAL_CONDOMINIO);
        Agendamento pendente = new Agendamento(
                "PP-2026-0021", cliente, LocalDate.of(2026, 9, 11),
                LocalTime.of(16, 0), LocalTime.of(17, 0),
                TipoServico.MANUTENCAO_PREVENTIVA, "Av. B, 500", "Quadro de bombas"
        );
        pendente.setId(21L);
        pendente.setStatus(StatusAgendamento.AGUARDANDO_APROVACAO);

        when(agendamentoRepository.findById(21L)).thenReturn(Optional.of(pendente));
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Agendamento recusado = agendamentoService.recusarSolicitacao(21L, "Leonardo", "Manutenção emergencial em subestação neste horário");

        assertEquals(StatusAgendamento.RECUSADO, recusado.getStatus());
        assertEquals("Recusado", recusado.getStatus().getDescricao());
        assertFalse(recusado.getStatus().ocupaHorarioNaAgenda());
        verify(historicoRepository).save(any(HistoricoStatusAgendamento.class));
        verify(notificacaoGateway).notificarAgendamentoRecusado(eq(recusado), contains("emergencial"));
    }

    @Test
    @DisplayName("Impede concluir visita sem aprovação prévia ou aprovar visita recusada")
    void deveImpedirConclusaoSemAprovacaoPreviaOuAprovarRecusada() {
        Cliente cliente = new Cliente("Paulo Silva", "16998887766", "Rua C, 30", PerfilCliente.RESIDENCIAL);
        Agendamento pendente = new Agendamento(
                "PP-2026-0022", cliente, LocalDate.of(2026, 9, 12),
                LocalTime.of(9, 0), LocalTime.of(10, 0),
                TipoServico.VISTORIA_ORCAMENTO, "Rua C, 30", "Vistoria"
        );
        pendente.setId(22L);
        pendente.setStatus(StatusAgendamento.AGUARDANDO_APROVACAO);

        when(agendamentoRepository.findById(22L)).thenReturn(Optional.of(pendente));

        assertThrows(RegraNegocioException.class, () -> agendamentoService.atualizarStatusOperacional(
                22L, StatusAgendamento.CONCLUIDO, "Leonardo", "Sem aprovar antes"
        ));

        pendente.setStatus(StatusAgendamento.RECUSADO);
        assertThrows(RegraNegocioException.class, () -> agendamentoService.aprovarSolicitacao(
                22L, "Leonardo", "Tentar aprovar recusada"
        ));
        verify(agendamentoRepository, never()).save(any());
    }
}

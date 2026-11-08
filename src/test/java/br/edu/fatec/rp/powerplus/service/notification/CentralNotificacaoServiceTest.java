package br.edu.fatec.rp.powerplus.service.notification;

import br.edu.fatec.rp.powerplus.domain.*;
import br.edu.fatec.rp.powerplus.repository.NotificacaoRegistroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CentralNotificacaoServiceTest {

    @Mock
    private NotificacaoRegistroRepository repository;

    private Clock clock;

    @BeforeEach
    void setUp() {
        ZoneId zona = ZoneId.of("America/Sao_Paulo");
        clock = Clock.fixed(LocalDateTime.of(2026, 9, 10, 10, 0).atZone(zona).toInstant(), zona);
    }

    @Test
    @DisplayName("Registra envio pelo canal interno quando integração WhatsApp está desligada")
    void deveUsarCanalInternoQuandoFeatureFlagDesativada() {
        CentralNotificacaoService service = new CentralNotificacaoService(repository, clock, false);
        assertFalse(service.isIntegracaoWhatsAppAtiva());

        Cliente cli = new Cliente("Mariana", "16991234567", "Rua X, 10", PerfilCliente.RESIDENCIAL);
        Agendamento ag = new Agendamento("PP-2026-0200", cli, LocalDate.of(2026, 9, 12),
                LocalTime.of(14, 0), LocalTime.of(15, 0), TipoServico.REPARO_ELETRICO, "Rua X, 10", "Teste");

        service.notificarAgendamentoConfirmado(ag);

        ArgumentCaptor<NotificacaoRegistro> captor = ArgumentCaptor.forClass(NotificacaoRegistro.class);
        verify(repository, times(2)).save(captor.capture());
        List<NotificacaoRegistro> salvas = captor.getAllValues();
        assertEquals("SISTEMA_INTERNO", salvas.get(0).getCanalEnvio());
        assertEquals("CLIENTE", salvas.get(0).getDestinatarioPapel());
        assertEquals("PROPRIETARIO", salvas.get(1).getDestinatarioPapel());
    }

    @Test
    @DisplayName("Habilita canal WhatsApp quando a flag de integração está ativa")
    void deveHabilitarCanalWhatsAppQuandoFeatureFlagAtiva() {
        CentralNotificacaoService service = new CentralNotificacaoService(repository, clock, true);
        assertTrue(service.isIntegracaoWhatsAppAtiva());

        Cliente cli = new Cliente("Condomínio Sol", "1636201010", "Av. Y, 50", PerfilCliente.EMPRESARIAL_CONDOMINIO);
        Agendamento ag = new Agendamento("PP-2026-0201", cli, LocalDate.of(2026, 9, 12),
                LocalTime.of(9, 0), LocalTime.of(10, 0), TipoServico.MANUTENCAO_PREVENTIVA, "Av. Y, 50", null);

        service.enviarLembreteVisita(ag);

        ArgumentCaptor<NotificacaoRegistro> captor = ArgumentCaptor.forClass(NotificacaoRegistro.class);
        verify(repository).save(captor.capture());
        assertEquals("SISTEMA_E_WHATSAPP_FLAG_ON", captor.getValue().getCanalEnvio());
        assertEquals(TipoNotificacao.LEMBRETE, captor.getValue().getTipo());
    }

    @Test
    @DisplayName("Valida regras de transição entre os status de agendamento")
    void deveValidarTodasAsTransicoesDoEnumStatusAgendamento() {
        assertTrue(StatusAgendamento.AGUARDANDO_APROVACAO.podeTransitarPara(StatusAgendamento.CONFIRMADO));
        assertTrue(StatusAgendamento.AGUARDANDO_APROVACAO.podeTransitarPara(StatusAgendamento.RECUSADO));
        assertTrue(StatusAgendamento.AGUARDANDO_APROVACAO.podeTransitarPara(StatusAgendamento.CANCELADO));
        assertFalse(StatusAgendamento.AGUARDANDO_APROVACAO.podeTransitarPara(StatusAgendamento.CONCLUIDO));
        assertFalse(StatusAgendamento.AGUARDANDO_APROVACAO.podeTransitarPara(StatusAgendamento.EM_ATENDIMENTO));

        assertTrue(StatusAgendamento.CONFIRMADO.podeTransitarPara(StatusAgendamento.EM_ATENDIMENTO));
        assertTrue(StatusAgendamento.CONFIRMADO.podeTransitarPara(StatusAgendamento.CONCLUIDO));
        assertTrue(StatusAgendamento.CONFIRMADO.podeTransitarPara(StatusAgendamento.CANCELADO));
        assertFalse(StatusAgendamento.CONFIRMADO.podeTransitarPara(StatusAgendamento.CONFIRMADO));
        assertFalse(StatusAgendamento.CONFIRMADO.podeTransitarPara(null));

        assertTrue(StatusAgendamento.EM_ATENDIMENTO.podeTransitarPara(StatusAgendamento.CONCLUIDO));
        assertTrue(StatusAgendamento.EM_ATENDIMENTO.podeTransitarPara(StatusAgendamento.CANCELADO));
        assertFalse(StatusAgendamento.EM_ATENDIMENTO.podeTransitarPara(StatusAgendamento.CONFIRMADO));

        assertFalse(StatusAgendamento.CONCLUIDO.podeTransitarPara(StatusAgendamento.CANCELADO));
        assertFalse(StatusAgendamento.CANCELADO.podeTransitarPara(StatusAgendamento.EM_ATENDIMENTO));
        assertFalse(StatusAgendamento.RECUSADO.podeTransitarPara(StatusAgendamento.CONFIRMADO));

        assertTrue(StatusAgendamento.AGUARDANDO_APROVACAO.ocupaHorarioNaAgenda());
        assertTrue(StatusAgendamento.CONFIRMADO.ocupaHorarioNaAgenda());
        assertTrue(StatusAgendamento.EM_ATENDIMENTO.ocupaHorarioNaAgenda());
        assertFalse(StatusAgendamento.CONCLUIDO.ocupaHorarioNaAgenda());
        assertFalse(StatusAgendamento.RECUSADO.ocupaHorarioNaAgenda());
        assertFalse(StatusAgendamento.CANCELADO.ocupaHorarioNaAgenda());
    }

    @Test
    @DisplayName("Registra avisos de nova solicitação e de recusa")
    void deveRegistrarNotificacoesDeSolicitacaoERecusa() {
        CentralNotificacaoService service = new CentralNotificacaoService(repository, clock, false);
        Cliente cli = new Cliente("Roberto", "16992223344", "Av. Z, 100", PerfilCliente.EMPRESARIAL_CONDOMINIO);
        Agendamento ag = new Agendamento("PP-2026-0205", cli, LocalDate.of(2026, 9, 14),
                LocalTime.of(15, 0), LocalTime.of(16, 0), TipoServico.QUADRO_DISJUNTORES, "Av. Z, 100", "QGBT");
        ag.setStatus(StatusAgendamento.AGUARDANDO_APROVACAO);

        service.notificarSolicitacaoRecebida(ag);
        service.notificarAgendamentoRecusado(ag, "Atendimento emergencial externo");

        ArgumentCaptor<NotificacaoRegistro> captor = ArgumentCaptor.forClass(NotificacaoRegistro.class);
        verify(repository, times(4)).save(captor.capture());
        List<NotificacaoRegistro> salvas = captor.getAllValues();
        assertEquals(TipoNotificacao.SOLICITACAO_RECEBIDA, salvas.get(0).getTipo());
        assertEquals(TipoNotificacao.RECUSA, salvas.get(2).getTipo());
    }
}

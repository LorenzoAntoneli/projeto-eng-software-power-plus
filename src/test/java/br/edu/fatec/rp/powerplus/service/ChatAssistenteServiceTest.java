package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.ConversaChat;
import br.edu.fatec.rp.powerplus.domain.StatusConversaChat;
import br.edu.fatec.rp.powerplus.repository.ConversaChatRepository;
import br.edu.fatec.rp.powerplus.service.notification.NotificacaoGateway;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatAssistenteServiceTest {

    @Mock
    private ConversaChatRepository conversaRepository;
    @Mock
    private DisponibilidadeService disponibilidadeService;
    @Mock
    private NotificacaoGateway notificacaoGateway;

    private ChatAssistenteService chatService;

    @BeforeEach
    void setUp() {
        ZoneId zona = ZoneId.of("America/Sao_Paulo");
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 9, 10, 10, 0).atZone(zona).toInstant(), zona);
        chatService = new ChatAssistenteService(conversaRepository, disponibilidadeService, notificacaoGateway, clock);
    }

    @Test
    @DisplayName("TU13 - Deve consultar horários livres reais ao responder pergunta sobre agenda no chat (HU05/UC03)")
    void deveResponderHorariosLivresReaisNoChat() {
        when(disponibilidadeService.consultarHorariosLivres(LocalDate.of(2026, 9, 10)))
                .thenReturn(List.of(LocalTime.of(14, 0), LocalTime.of(16, 0)));
        when(disponibilidadeService.consultarHorariosLivres(LocalDate.of(2026, 9, 11)))
                .thenReturn(List.of(LocalTime.of(9, 0)));
        when(conversaRepository.save(any(ConversaChat.class))).thenAnswer(inv -> inv.getArgument(0));

        ConversaChat conversa = chatService.iniciarOuEnviarMensagemCliente(
                null, "Fernanda Lima", "(16) 99333-4455", "Quais horários disponíveis vocês têm hoje?", false
        );

        assertEquals(StatusConversaChat.AUTOMATICO, conversa.getStatus());
        String ultimaResposta = conversa.getMensagens().get(conversa.getMensagens().size() - 1).getConteudo();
        assertTrue(ultimaResposta.contains("14:00"));
        assertTrue(ultimaResposta.contains("10/09"));
        verify(notificacaoGateway, never()).notificarTransbordoChat(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("TU14 - Deve encaminhar conversa para o proprietário quando assunto for complexo ou solicitado (HU11/UC03 2a)")
    void deveEncaminharConversaParaProprietarioComHistorico() {
        when(conversaRepository.save(any(ConversaChat.class))).thenAnswer(inv -> {
            ConversaChat c = inv.getArgument(0);
            c.setId(55L);
            return c;
        });

        ConversaChat conversa = chatService.iniciarOuEnviarMensagemCliente(
                null, "Síndico Roberto", "(16) 3622-8899",
                "Preciso dimensionar barramento trifásico de 150A para gerador no subsolo", false
        );

        assertEquals(StatusConversaChat.AGUARDANDO_HUMANO, conversa.getStatus());
        verify(notificacaoGateway).notificarTransbordoChat(eq(55L), eq("Síndico Roberto"), contains("barramento trifásico"));
    }

    @Test
    @DisplayName("TU15 - Deve permitir que o proprietário assuma e responda conversa em transbordo (HU11)")
    void devePermitirProprietarioAssumirConversa() {
        ConversaChat existente = new ConversaChat("Síndico Roberto", "1636228899", LocalDateTime.of(2026, 9, 10, 9, 50));
        existente.setId(55L);
        existente.setStatus(StatusConversaChat.AGUARDANDO_HUMANO);

        when(conversaRepository.findById(55L)).thenReturn(Optional.of(existente));
        when(conversaRepository.save(any(ConversaChat.class))).thenAnswer(inv -> inv.getArgument(0));

        ConversaChat atualizada = chatService.responderComoProprietario(
                55L, "Bom dia, Roberto! Faço esse dimensionamento sim. Posso ir amanhã às 09h avaliar o QGBT.", false
        );

        assertEquals(StatusConversaChat.EM_ATENDIMENTO_HUMANO, atualizada.getStatus());
        assertEquals("PROPRIETARIO", atualizada.getMensagens().get(atualizada.getMensagens().size() - 1).getRemetente());
    }
}

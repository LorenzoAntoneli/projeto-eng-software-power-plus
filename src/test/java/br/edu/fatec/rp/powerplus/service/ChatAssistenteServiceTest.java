package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.ConversaChat;
import br.edu.fatec.rp.powerplus.domain.StatusConversaChat;
import br.edu.fatec.rp.powerplus.repository.ConversaChatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatAssistenteServiceTest {

    @Mock private ConversaChatRepository conversaRepository;
    @Mock private DisponibilidadeService disponibilidadeService;
    @Mock private NotificacaoService notificacaoService;
    @InjectMocks private ChatAssistenteService chatService;

    @Test
    @DisplayName("Responde próximos horários livres quando o cliente pergunta sobre agenda")
    void deveResponderHorariosLivresReaisNoChat() {
        LocalDate hoje = LocalDate.now();
        when(disponibilidadeService.consultarHorariosLivres(hoje))
                .thenReturn(List.of(LocalTime.of(14, 0), LocalTime.of(16, 0)));
        when(disponibilidadeService.consultarHorariosLivres(hoje.plusDays(1)))
                .thenReturn(List.of(LocalTime.of(9, 0)));
        when(conversaRepository.save(any(ConversaChat.class))).thenAnswer(inv -> inv.getArgument(0));

        ConversaChat conversa = chatService.iniciarOuEnviarMensagemCliente(
                null, "Fernanda Lima", "(16) 99333-4455", "Quais horários disponíveis vocês têm hoje?", false
        );

        assertEquals(StatusConversaChat.AUTOMATICO, conversa.getStatus());
        String ultimaResposta = conversa.getMensagens().get(conversa.getMensagens().size() - 1).getConteudo();
        assertTrue(ultimaResposta.contains("14:00"));
        assertTrue(ultimaResposta.contains(hoje.format(DateTimeFormatter.ofPattern("dd/MM"))));
        verify(notificacaoService, never()).notificarTransbordoChat(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Encaminha a conversa para o proprietário quando o assunto foge das dúvidas comuns")
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
        verify(notificacaoService).notificarTransbordoChat(eq(55L), eq("Síndico Roberto"), contains("barramento trifásico"));
    }

    @Test
    @DisplayName("Permite ao proprietário assumir e responder conversa pendente")
    void devePermitirProprietarioAssumirConversa() {
        ConversaChat existente = new ConversaChat("Síndico Roberto", "1636228899", LocalDateTime.now().minusMinutes(10));
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

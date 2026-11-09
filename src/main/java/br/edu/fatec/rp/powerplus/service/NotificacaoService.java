package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.Agendamento;
import br.edu.fatec.rp.powerplus.domain.NotificacaoRegistro;
import br.edu.fatec.rp.powerplus.domain.TipoNotificacao;
import br.edu.fatec.rp.powerplus.repository.NotificacaoRegistroRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Registra avisos internos (e flag de WhatsApp) para o cliente e para o eletricista.
 */
@Service
public class NotificacaoService {

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificacaoRegistroRepository repository;
    private final boolean whatsappEnabled;

    public NotificacaoService(NotificacaoRegistroRepository repository,
                              @Value("${powerplus.features.whatsapp-enabled:false}") boolean whatsappEnabled) {
        this.repository = repository;
        this.whatsappEnabled = whatsappEnabled;
    }

    @Transactional
    public void notificarSolicitacaoRecebida(Agendamento a) {
        String dh = formatarDataHora(a);
        avisarAmbos(TipoNotificacao.SOLICITACAO_RECEBIDA, a.getProtocolo(),
                "Solicitação recebida — Aguardando aprovação (" + a.getProtocolo() + ")",
                "Olá " + a.getCliente().getNome() + "! Recebemos seu pedido para " + dh + ". O horário está pré-reservado e aguarda aprovação do Leonardo.",
                "Aprovação pendente: " + a.getCliente().getNome(),
                "Protocolo " + a.getProtocolo() + " solicitou visita em " + dh + " – " + a.getTipoServico().getDescricao() + ". Aprove ou recuse no painel.");
    }

    @Transactional
    public void notificarAgendamentoConfirmado(Agendamento a) {
        String dh = formatarDataHora(a);
        avisarAmbos(TipoNotificacao.CONFIRMACAO, a.getProtocolo(),
                "Agendamento confirmado (" + a.getProtocolo() + ")",
                "Olá " + a.getCliente().getNome() + "! Sua visita elétrica (" + a.getTipoServico().getDescricao() + ") foi confirmada para " + dh + " em " + a.getEnderecoAtendimento() + ".",
                "Novo agendamento: " + a.getCliente().getNome(),
                "Protocolo " + a.getProtocolo() + " marcado para " + dh + " – " + a.getTipoServico().getDescricao() + " (" + a.getEnderecoAtendimento() + ").");
    }

    @Transactional
    public void notificarAgendamentoRecusado(Agendamento a, String motivo) {
        String dh = formatarDataHora(a);
        String detalhe = (motivo != null && !motivo.isBlank()) ? motivo : "Indisponibilidade técnica no horário";
        avisarAmbos(TipoNotificacao.RECUSA, a.getProtocolo(),
                "Solicitação não aprovada (" + a.getProtocolo() + ")",
                "Não foi possível aprovar o horário de " + dh + " (" + detalhe + "). Por favor, escolha outro horário livre na agenda.",
                "Solicitação recusada: " + a.getCliente().getNome(),
                "Horário de " + dh + " liberado após recusa do protocolo " + a.getProtocolo() + " (" + detalhe + ").");
    }

    @Transactional
    public void notificarAgendamentoRemarcado(Agendamento a, String horarioAnterior) {
        String dh = formatarDataHora(a);
        avisarAmbos(TipoNotificacao.REMARCACAO, a.getProtocolo(),
                "Visita remarcada (" + a.getProtocolo() + ")",
                "Seu atendimento anterior (" + horarioAnterior + ") foi remarcado para " + dh + ".",
                "Remarcação de horário: " + a.getCliente().getNome(),
                "Protocolo " + a.getProtocolo() + " alterado de " + horarioAnterior + " para " + dh + ".");
    }

    @Transactional
    public void notificarAgendamentoCancelado(Agendamento a, String origem) {
        String dh = formatarDataHora(a);
        avisarAmbos(TipoNotificacao.CANCELAMENTO, a.getProtocolo(),
                "Agendamento cancelado (" + a.getProtocolo() + ")",
                "O atendimento de " + dh + " foi cancelado (" + origem + ").",
                "Cancelamento na agenda: " + a.getCliente().getNome(),
                "Horário de " + dh + " liberado após cancelamento (" + origem + ") do protocolo " + a.getProtocolo() + ".");
    }

    @Transactional
    public void enviarLembreteVisita(Agendamento a) {
        salvar(TipoNotificacao.LEMBRETE, "CLIENTE",
                "Lembrete de visita técnica Power Plus (" + a.getProtocolo() + ")",
                "Lembrete: o eletricista realizará seu atendimento em " + formatarDataHora(a) + " no endereço " + a.getEnderecoAtendimento() + ".",
                a.getProtocolo(), canalAtivo());
    }

    @Transactional
    public void notificarTransbordoChat(Long conversaId, String nomeCliente, String ultimaMensagem) {
        salvar(TipoNotificacao.TRANSBORDO_CHAT, "PROPRIETARIO",
                "Cliente aguarda atendimento humano no chat",
                "Conversa #" + conversaId + " (" + nomeCliente + "): \"" + ultimaMensagem + "\"",
                "CHAT-" + conversaId, "SISTEMA_INTERNO");
    }

    public boolean isIntegracaoWhatsAppAtiva() {
        return whatsappEnabled;
    }

    @Transactional(readOnly = true)
    public List<NotificacaoRegistro> listarRecentes() {
        return repository.findTop50ByOrderByCriadaEmDesc();
    }

    @Transactional
    public void marcarTodasComoLidasProprietario() {
        repository.findTop50ByOrderByCriadaEmDesc().forEach(n -> {
            if ("PROPRIETARIO".equals(n.getDestinatarioPapel()) && !n.isLida()) {
                n.setLida(true);
                repository.save(n);
            }
        });
    }

    @Transactional(readOnly = true)
    public long contarNaoLidasProprietario() {
        return repository.countByDestinatarioPapelAndLidaFalse("PROPRIETARIO");
    }

    private void avisarAmbos(TipoNotificacao tipo, String protocolo, String titCli, String msgCli, String titProp, String msgProp) {
        String canal = canalAtivo();
        salvar(tipo, "CLIENTE", titCli, msgCli, protocolo, canal);
        salvar(tipo, "PROPRIETARIO", titProp, msgProp, protocolo, canal);
    }

    private void salvar(TipoNotificacao tipo, String papel, String titulo, String mensagem, String protocolo, String canal) {
        repository.save(new NotificacaoRegistro(tipo, papel, titulo, mensagem, protocolo, canal, LocalDateTime.now()));
    }

    private String formatarDataHora(Agendamento a) {
        return a.getData().format(FMT_DATA) + " às " + a.getHoraInicio().format(FMT_HORA);
    }

    private String canalAtivo() {
        return whatsappEnabled ? "SISTEMA_E_WHATSAPP_FLAG_ON" : "SISTEMA_INTERNO";
    }
}

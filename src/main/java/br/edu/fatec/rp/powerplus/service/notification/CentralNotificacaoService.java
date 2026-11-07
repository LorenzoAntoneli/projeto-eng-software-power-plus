package br.edu.fatec.rp.powerplus.service.notification;

import br.edu.fatec.rp.powerplus.domain.Agendamento;
import br.edu.fatec.rp.powerplus.domain.NotificacaoRegistro;
import br.edu.fatec.rp.powerplus.domain.TipoNotificacao;
import br.edu.fatec.rp.powerplus.repository.NotificacaoRegistroRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CentralNotificacaoService implements NotificacaoGateway {

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificacaoRegistroRepository repository;
    private final Clock clock;
    private final boolean whatsappEnabled;

    public CentralNotificacaoService(NotificacaoRegistroRepository repository,
                                     Clock clock,
                                     @Value("${powerplus.features.whatsapp-enabled:false}") boolean whatsappEnabled) {
        this.repository = repository;
        this.clock = clock;
        this.whatsappEnabled = whatsappEnabled;
    }

    @Override
    @Transactional
    public void notificarSolicitacaoRecebida(Agendamento a) {
        LocalDateTime agora = LocalDateTime.now(clock);
        String canal = canalAtivo();
        String dataHora = a.getData().format(FMT_DATA) + " às " + a.getHoraInicio().format(FMT_HORA);

        repository.save(new NotificacaoRegistro(
                TipoNotificacao.SOLICITACAO_RECEBIDA,
                "CLIENTE",
                "Solicitação recebida — Aguardando aprovação (" + a.getProtocolo() + ")",
                "Olá " + a.getCliente().getNome() + "! Recebemos seu pedido para " + dataHora
                        + ". O horário está pré-reservado e aguarda aprovação do Leonardo.",
                a.getProtocolo(),
                canal,
                agora
        ));

        repository.save(new NotificacaoRegistro(
                TipoNotificacao.SOLICITACAO_RECEBIDA,
                "PROPRIETARIO",
                "Aprovação pendente: " + a.getCliente().getNome(),
                "Protocolo " + a.getProtocolo() + " solicitou visita em " + dataHora + " – "
                        + a.getTipoServico().getDescricao() + ". Aprove ou recuse no painel.",
                a.getProtocolo(),
                canal,
                agora
        ));
    }

    @Override
    @Transactional
    public void notificarAgendamentoRecusado(Agendamento a, String motivo) {
        LocalDateTime agora = LocalDateTime.now(clock);
        String canal = canalAtivo();
        String dataHora = a.getData().format(FMT_DATA) + " às " + a.getHoraInicio().format(FMT_HORA);
        String detalhe = (motivo != null && !motivo.isBlank()) ? motivo : "Indisponibilidade técnica no horário";

        repository.save(new NotificacaoRegistro(
                TipoNotificacao.RECUSA,
                "CLIENTE",
                "Solicitação não aprovada (" + a.getProtocolo() + ")",
                "Não foi possível aprovar o horário de " + dataHora + " (" + detalhe
                        + "). Por favor, escolha outro horário livre na agenda.",
                a.getProtocolo(),
                canal,
                agora
        ));

        repository.save(new NotificacaoRegistro(
                TipoNotificacao.RECUSA,
                "PROPRIETARIO",
                "Solicitação recusada: " + a.getCliente().getNome(),
                "Horário de " + dataHora + " liberado após recusa do protocolo " + a.getProtocolo() + " (" + detalhe + ").",
                a.getProtocolo(),
                canal,
                agora
        ));
    }

    @Override
    @Transactional
    public void notificarAgendamentoConfirmado(Agendamento a) {
        LocalDateTime agora = LocalDateTime.now(clock);
        String canal = canalAtivo();
        String dataHora = a.getData().format(FMT_DATA) + " às " + a.getHoraInicio().format(FMT_HORA);

        repository.save(new NotificacaoRegistro(
                TipoNotificacao.CONFIRMACAO,
                "CLIENTE",
                "Agendamento confirmado (" + a.getProtocolo() + ")",
                "Olá " + a.getCliente().getNome() + "! Sua visita elétrica (" + a.getTipoServico().getDescricao()
                        + ") foi confirmada para " + dataHora + " em " + a.getEnderecoAtendimento() + ".",
                a.getProtocolo(),
                canal,
                agora
        ));

        repository.save(new NotificacaoRegistro(
                TipoNotificacao.CONFIRMACAO,
                "PROPRIETARIO",
                "Novo agendamento: " + a.getCliente().getNome(),
                "Protocolo " + a.getProtocolo() + " marcado para " + dataHora + " – "
                        + a.getTipoServico().getDescricao() + " (" + a.getEnderecoAtendimento() + ").",
                a.getProtocolo(),
                canal,
                agora
        ));
    }

    @Override
    @Transactional
    public void notificarAgendamentoRemarcado(Agendamento a, String horarioAnterior) {
        LocalDateTime agora = LocalDateTime.now(clock);
        String canal = canalAtivo();
        String novaDataHora = a.getData().format(FMT_DATA) + " às " + a.getHoraInicio().format(FMT_HORA);

        repository.save(new NotificacaoRegistro(
                TipoNotificacao.REMARCACAO,
                "CLIENTE",
                "Visita remarcada (" + a.getProtocolo() + ")",
                "Seu atendimento anterior (" + horarioAnterior + ") foi remarcado para " + novaDataHora + ".",
                a.getProtocolo(),
                canal,
                agora
        ));

        repository.save(new NotificacaoRegistro(
                TipoNotificacao.REMARCACAO,
                "PROPRIETARIO",
                "Remarcação de horário: " + a.getCliente().getNome(),
                "Protocolo " + a.getProtocolo() + " alterado de " + horarioAnterior + " para " + novaDataHora + ".",
                a.getProtocolo(),
                canal,
                agora
        ));
    }

    @Override
    @Transactional
    public void notificarAgendamentoCancelado(Agendamento a, String origem) {
        LocalDateTime agora = LocalDateTime.now(clock);
        String canal = canalAtivo();
        String dataHora = a.getData().format(FMT_DATA) + " às " + a.getHoraInicio().format(FMT_HORA);

        repository.save(new NotificacaoRegistro(
                TipoNotificacao.CANCELAMENTO,
                "CLIENTE",
                "Agendamento cancelado (" + a.getProtocolo() + ")",
                "O atendimento de " + dataHora + " foi cancelado (" + origem + ").",
                a.getProtocolo(),
                canal,
                agora
        ));

        repository.save(new NotificacaoRegistro(
                TipoNotificacao.CANCELAMENTO,
                "PROPRIETARIO",
                "Cancelamento na agenda: " + a.getCliente().getNome(),
                "Horário de " + dataHora + " liberado após cancelamento (" + origem + ") do protocolo " + a.getProtocolo() + ".",
                a.getProtocolo(),
                canal,
                agora
        ));
    }

    @Override
    @Transactional
    public void enviarLembreteVisita(Agendamento a) {
        LocalDateTime agora = LocalDateTime.now(clock);
        String dataHora = a.getData().format(FMT_DATA) + " às " + a.getHoraInicio().format(FMT_HORA);
        repository.save(new NotificacaoRegistro(
                TipoNotificacao.LEMBRETE,
                "CLIENTE",
                "Lembrete de visita técnica Power Plus (" + a.getProtocolo() + ")",
                "Lembrete: o eletricista realizará seu atendimento em " + dataHora + " no endereço " + a.getEnderecoAtendimento() + ".",
                a.getProtocolo(),
                canalAtivo(),
                agora
        ));
    }

    @Override
    @Transactional
    public void notificarTransbordoChat(Long conversaId, String nomeCliente, String ultimaMensagem) {
        repository.save(new NotificacaoRegistro(
                TipoNotificacao.TRANSBORDO_CHAT,
                "PROPRIETARIO",
                "Cliente aguarda atendimento humano no chat",
                "Conversa #" + conversaId + " (" + nomeCliente + "): \"" + ultimaMensagem + "\"",
                "CHAT-" + conversaId,
                "SISTEMA_INTERNO",
                LocalDateTime.now(clock)
        ));
    }

    @Override
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

    private String canalAtivo() {
        return whatsappEnabled ? "SISTEMA_E_WHATSAPP_FLAG_ON" : "SISTEMA_INTERNO";
    }
}

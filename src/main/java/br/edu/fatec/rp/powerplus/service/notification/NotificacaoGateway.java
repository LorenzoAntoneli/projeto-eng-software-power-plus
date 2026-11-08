package br.edu.fatec.rp.powerplus.service.notification;

import br.edu.fatec.rp.powerplus.domain.Agendamento;

public interface NotificacaoGateway {
    void notificarSolicitacaoRecebida(Agendamento agendamento);
    void notificarAgendamentoConfirmado(Agendamento agendamento);
    void notificarAgendamentoRecusado(Agendamento agendamento, String motivo);
    void notificarAgendamentoRemarcado(Agendamento agendamento, String horarioAnterior);
    void notificarAgendamentoCancelado(Agendamento agendamento, String origem);
    void enviarLembreteVisita(Agendamento agendamento);
    void notificarTransbordoChat(Long conversaId, String nomeCliente, String ultimaMensagem);
    boolean isIntegracaoWhatsAppAtiva();
}

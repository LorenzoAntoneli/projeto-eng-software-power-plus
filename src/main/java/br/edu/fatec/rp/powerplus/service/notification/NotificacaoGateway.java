package br.edu.fatec.rp.powerplus.service.notification;

import br.edu.fatec.rp.powerplus.domain.Agendamento;

public interface NotificacaoGateway {
    void notificarAgendamentoConfirmado(Agendamento agendamento);
    void notificarAgendamentoRemarcado(Agendamento agendamento, String horarioAnterior);
    void notificarAgendamentoCancelado(Agendamento agendamento, String origem);
    void enviarLembreteVisita(Agendamento agendamento);
    void notificarTransbordoChat(Long conversaId, String nomeCliente, String ultimaMensagem);
    boolean isIntegracaoWhatsAppAtiva();
}

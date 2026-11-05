package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.*;
import br.edu.fatec.rp.powerplus.repository.*;
import br.edu.fatec.rp.powerplus.service.notification.NotificacaoGateway;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Etapa RED do TDD: esqueleto inicial antes da implementação das regras RN01..RN04.
 */
@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final DisponibilidadeSemanalRepository disponibilidadeRepository;
    private final BloqueioAgendaRepository bloqueioRepository;
    private final HistoricoStatusRepository historicoRepository;
    private final ClienteService clienteService;
    private final NotificacaoGateway notificacaoGateway;
    private final Clock clock;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              DisponibilidadeSemanalRepository disponibilidadeRepository,
                              BloqueioAgendaRepository bloqueioRepository,
                              HistoricoStatusRepository historicoRepository,
                              ClienteService clienteService,
                              NotificacaoGateway notificacaoGateway,
                              Clock clock) {
        this.agendamentoRepository = agendamentoRepository;
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.historicoRepository = historicoRepository;
        this.clienteService = clienteService;
        this.notificacaoGateway = notificacaoGateway;
        this.clock = clock;
    }

    public Agendamento agendarAutoatendimento(String nomeCliente, String telefone, String email,
                                              String endereco, PerfilCliente perfil,
                                              LocalDate data, LocalTime horaInicio,
                                              TipoServico tipoServico, String descricao) {
        throw new UnsupportedOperationException("RED: regra de agendamento ainda não implementada");
    }

    public Agendamento cancelarPeloCliente(String protocolo, String motivo) {
        throw new UnsupportedOperationException("RED: cancelamento com antecedência mínima ainda não implementado");
    }

    public Agendamento atualizarStatusOperacional(Long agendamentoId, StatusAgendamento novoStatus,
                                                  String responsavel, String observacao) {
        throw new UnsupportedOperationException("RED: transição de status ainda não implementada");
    }
}

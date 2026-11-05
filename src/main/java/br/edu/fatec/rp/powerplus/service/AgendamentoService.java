package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.*;
import br.edu.fatec.rp.powerplus.domain.exception.ConflitoHorarioException;
import br.edu.fatec.rp.powerplus.domain.exception.RecursoNaoEncontradoException;
import br.edu.fatec.rp.powerplus.domain.exception.RegraNegocioException;
import br.edu.fatec.rp.powerplus.repository.*;
import br.edu.fatec.rp.powerplus.service.notification.NotificacaoGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Etapa GREEN do TDD: implementação funcional fazendo todos os testes passarem.
 */
@Service
public class AgendamentoService {

    private static final List<StatusAgendamento> STATUS_QUE_OCUPAM_AGENDA =
            List.of(StatusAgendamento.CONFIRMADO, StatusAgendamento.EM_ATENDIMENTO);

    private final AgendamentoRepository agendamentoRepository;
    private final DisponibilidadeSemanalRepository disponibilidadeRepository;
    private final BloqueioAgendaRepository bloqueioRepository;
    private final HistoricoStatusRepository historicoRepository;
    private final ClienteService clienteService;
    private final NotificacaoGateway notificacaoGateway;
    private final Clock clock;
    private final AtomicLong sequencialProtocolo = new AtomicLong(100);

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

    @Transactional
    public Agendamento agendarAutoatendimento(String nomeCliente, String telefone, String email,
                                              String endereco, PerfilCliente perfil,
                                              LocalDate data, LocalTime horaInicio,
                                              TipoServico tipoServico, String descricao) {
        if (data == null || horaInicio == null || tipoServico == null) {
            throw new RegraNegocioException("Data, horário e tipo de serviço são obrigatórios.");
        }
        LocalDateTime agora = LocalDateTime.now(clock);
        if (!LocalDateTime.of(data, horaInicio).isAfter(agora)) {
            throw new RegraNegocioException("O agendamento deve ser realizado para uma data e horário futuros.");
        }
        if (bloqueioRepository.existsByData(data)) {
            throw new RegraNegocioException("A data selecionada encontra-se bloqueada na agenda do eletricista.");
        }
        DisponibilidadeSemanal disp = disponibilidadeRepository.findByDiaSemana(data.getDayOfWeek())
                .filter(DisponibilidadeSemanal::isAtivo)
                .orElseThrow(() -> new RegraNegocioException("Não há expediente configurado para o dia da semana selecionado."));

        LocalTime horaFim = horaInicio.plusMinutes(disp.getDuracaoSlotMinutos());
        if (horaInicio.isBefore(disp.getHoraInicio()) || horaFim.isAfter(disp.getHoraFim())) {
            throw new RegraNegocioException("O horário escolhido está fora do expediente configurado.");
        }

        if (agendamentoRepository.existeConflitoNoHorario(data, horaInicio, STATUS_QUE_OCUPAM_AGENDA, null)) {
            throw new ConflitoHorarioException("O horário selecionado já está ocupado por outro atendimento.");
        }

        Cliente cliente = clienteService.obterOuCadastrarNoAgendamento(nomeCliente, telefone, email, endereco, perfil);
        String protocolo = String.format("PP-%d-%04d", data.getYear(), sequencialProtocolo.incrementAndGet());

        Agendamento novo = new Agendamento(
                protocolo,
                cliente,
                data,
                horaInicio,
                horaFim,
                tipoServico,
                endereco.trim(),
                descricao != null ? descricao.trim() : null
        );
        novo.setCriadoEm(agora);
        novo.setAtualizadoEm(agora);
        Agendamento salvo = agendamentoRepository.save(novo);

        historicoRepository.save(new HistoricoStatusAgendamento(
                salvo, null, StatusAgendamento.CONFIRMADO, "CLIENTE (Autoatendimento)",
                "Agendamento criado pelo cliente", agora
        ));
        notificacaoGateway.notificarAgendamentoConfirmado(salvo);
        return salvo;
    }

    @Transactional
    public Agendamento cancelarPeloCliente(String protocolo, String motivo) {
        Agendamento agendamento = agendamentoRepository.findByProtocoloIgnoreCase(protocolo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Protocolo não encontrado: " + protocolo));

        if (!agendamento.getStatus().podeTransitarPara(StatusAgendamento.CANCELADO)) {
            throw new RegraNegocioException("Este agendamento não pode mais ser cancelado (status atual: "
                    + agendamento.getStatus().getDescricao() + ").");
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        Duration antecedencia = Duration.between(agora, agendamento.getDataHoraInicio());
        if (antecedencia.toMinutes() < 120) {
            throw new RegraNegocioException(
                    "Cancelamentos ou remarcações pelo sistema exigem no mínimo 2 horas de antecedência. " +
                    "Por favor, fale diretamente com o proprietário no chat.");
        }

        StatusAgendamento anterior = agendamento.getStatus();
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamento.setAtualizadoEm(agora);
        Agendamento salvo = agendamentoRepository.save(agendamento);

        historicoRepository.save(new HistoricoStatusAgendamento(
                salvo, anterior, StatusAgendamento.CANCELADO, "CLIENTE",
                motivo != null && !motivo.isBlank() ? motivo.trim() : "Cancelado pelo cliente", agora
        ));
        notificacaoGateway.notificarAgendamentoCancelado(salvo, "CLIENTE");
        return salvo;
    }

    @Transactional
    public Agendamento atualizarStatusOperacional(Long agendamentoId, StatusAgendamento novoStatus,
                                                  String responsavel, String observacao) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado: " + agendamentoId));

        if (!agendamento.getStatus().podeTransitarPara(novoStatus)) {
            throw new RegraNegocioException(String.format(
                    "Transição inválida de status: não é permitido alterar de %s para %s.",
                    agendamento.getStatus().getDescricao(),
                    novoStatus != null ? novoStatus.getDescricao() : "NULO"
            ));
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        StatusAgendamento anterior = agendamento.getStatus();
        agendamento.setStatus(novoStatus);
        if (observacao != null && !observacao.isBlank()) {
            agendamento.setObservacaoTecnica(observacao.trim());
        }
        agendamento.setAtualizadoEm(agora);
        Agendamento salvo = agendamentoRepository.save(agendamento);

        historicoRepository.save(new HistoricoStatusAgendamento(
                salvo, anterior, novoStatus,
                responsavel != null && !responsavel.isBlank() ? responsavel : "PROPRIETARIO",
                observacao, agora
        ));
        if (novoStatus == StatusAgendamento.CANCELADO) {
            notificacaoGateway.notificarAgendamentoCancelado(salvo, "PROPRIETARIO");
        }
        return salvo;
    }
}

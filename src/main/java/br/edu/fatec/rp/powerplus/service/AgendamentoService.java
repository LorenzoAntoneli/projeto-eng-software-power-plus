package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.*;
import br.edu.fatec.rp.powerplus.domain.exception.RecursoNaoEncontradoException;
import br.edu.fatec.rp.powerplus.domain.exception.RegraNegocioException;
import br.edu.fatec.rp.powerplus.repository.AgendamentoRepository;
import br.edu.fatec.rp.powerplus.repository.HistoricoStatusRepository;
import br.edu.fatec.rp.powerplus.service.notification.NotificacaoGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AgendamentoService {

    private static final DateTimeFormatter FMT_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AgendamentoRepository agendamentoRepository;
    private final HistoricoStatusRepository historicoRepository;
    private final ClienteService clienteService;
    private final PoliticaAgendamentoValidator validador;
    private final NotificacaoGateway notificacaoGateway;
    private final Clock clock;
    private final AtomicLong sequencialProtocolo = new AtomicLong(100);

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              HistoricoStatusRepository historicoRepository,
                              ClienteService clienteService,
                              PoliticaAgendamentoValidator validador,
                              NotificacaoGateway notificacaoGateway,
                              Clock clock) {
        this.agendamentoRepository = agendamentoRepository;
        this.historicoRepository = historicoRepository;
        this.clienteService = clienteService;
        this.validador = validador;
        this.notificacaoGateway = notificacaoGateway;
        this.clock = clock;
    }

    @Transactional
    public Agendamento agendarAutoatendimento(String nomeCliente, String telefone, String email,
                                              String endereco, PerfilCliente perfil,
                                              LocalDate data, LocalTime horaInicio,
                                              TipoServico tipoServico, String descricao) {
        if (tipoServico == null) {
            throw new RegraNegocioException("O tipo de serviço elétrico é obrigatório.");
        }
        LocalTime horaFim = validador.validarHorarioDisponivelECalcularFim(data, horaInicio, null);
        Cliente cliente = clienteService.obterOuCadastrarNoAgendamento(nomeCliente, telefone, email, endereco, perfil);

        LocalDateTime agora = LocalDateTime.now(clock);
        long proximoNumero = Math.max(sequencialProtocolo.incrementAndGet(), agendamentoRepository.count() + 101);
        String protocolo = String.format("PP-%d-%04d", data.getYear(), proximoNumero);

        Agendamento novo = new Agendamento(
                protocolo,
                cliente,
                data,
                horaInicio,
                horaFim,
                tipoServico,
                endereco.trim(),
                descricao != null && !descricao.isBlank() ? descricao.trim() : null
        );
        novo.setCriadoEm(agora);
        novo.setAtualizadoEm(agora);
        Agendamento salvo = agendamentoRepository.save(novo);

        historicoRepository.save(new HistoricoStatusAgendamento(
                salvo, null, StatusAgendamento.CONFIRMADO, "CLIENTE (Autoatendimento)",
                "Agendamento criado com sucesso", agora
        ));
        notificacaoGateway.notificarAgendamentoConfirmado(salvo);
        return salvo;
    }

    @Transactional
    public Agendamento remarcarPeloCliente(String protocolo, LocalDate novaData, LocalTime novaHoraInicio, String motivo) {
        Agendamento agendamento = buscarPorProtocolo(protocolo);
        if (agendamento.getStatus() != StatusAgendamento.CONFIRMADO) {
            throw new RegraNegocioException("Apenas agendamentos confirmados podem ser remarcados.");
        }

        validador.validarAntecedenciaAutoatendimento(agendamento);
        LocalTime novaHoraFim = validador.validarHorarioDisponivelECalcularFim(novaData, novaHoraInicio, agendamento.getId());

        String horarioAnterior = agendamento.getDataHoraInicio().format(FMT_DATA_HORA);
        LocalDateTime agora = LocalDateTime.now(clock);

        agendamento.setData(novaData);
        agendamento.setHoraInicio(novaHoraInicio);
        agendamento.setHoraFim(novaHoraFim);
        agendamento.setAtualizadoEm(agora);
        Agendamento salvo = agendamentoRepository.save(agendamento);

        historicoRepository.save(new HistoricoStatusAgendamento(
                salvo, StatusAgendamento.CONFIRMADO, StatusAgendamento.CONFIRMADO, "CLIENTE",
                "Remarcado de " + horarioAnterior + (motivo != null && !motivo.isBlank() ? " (" + motivo.trim() + ")" : ""),
                agora
        ));
        notificacaoGateway.notificarAgendamentoRemarcado(salvo, horarioAnterior);
        return salvo;
    }

    @Transactional
    public Agendamento cancelarPeloCliente(String protocolo, String motivo) {
        Agendamento agendamento = buscarPorProtocolo(protocolo);
        validador.validarTransicaoStatus(agendamento, StatusAgendamento.CANCELADO);
        validador.validarAntecedenciaAutoatendimento(agendamento);

        LocalDateTime agora = LocalDateTime.now(clock);
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

        validador.validarTransicaoStatus(agendamento, novoStatus);

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

    @Transactional
    public Agendamento aprovarSolicitacao(Long agendamentoId, String responsavel, String observacao) {
        return agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado: " + agendamentoId));
    }

    @Transactional
    public Agendamento recusarSolicitacao(Long agendamentoId, String responsavel, String motivo) {
        return agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado: " + agendamentoId));
    }

    @Transactional
    public void dispararLembreteManual(Long agendamentoId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado: " + agendamentoId));
        if (agendamento.getStatus() != StatusAgendamento.CONFIRMADO) {
            throw new RegraNegocioException("Lembretes só podem ser enviados para agendamentos confirmados.");
        }
        notificacaoGateway.enviarLembreteVisita(agendamento);
    }

    @Transactional(readOnly = true)
    public Agendamento buscarPorProtocolo(String protocolo) {
        if (protocolo == null || protocolo.isBlank()) {
            throw new RegraNegocioException("Informe o código de protocolo.");
        }
        return agendamentoRepository.findByProtocoloIgnoreCase(protocolo.trim())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Protocolo não encontrado: " + protocolo));
    }

    @Transactional(readOnly = true)
    public List<Agendamento> consultarPorTelefoneCliente(String telefone) {
        String tel = Cliente.normalizarTelefone(telefone);
        if (tel.length() < 10) {
            throw new RegraNegocioException("Informe um telefone válido com DDD para consulta.");
        }
        return agendamentoRepository.findByClienteTelefoneOrderByDataDescHoraInicioDesc(tel);
    }

    @Transactional(readOnly = true)
    public List<Agendamento> consultarHistoricoClienteId(Long clienteId) {
        return agendamentoRepository.findByClienteIdOrderByDataDescHoraInicioDesc(clienteId);
    }

    @Transactional(readOnly = true)
    public List<Agendamento> listarAgendaDia(LocalDate data) {
        return agendamentoRepository.findByDataOrderByHoraInicioAsc(data);
    }

    @Transactional(readOnly = true)
    public List<Agendamento> listarAgendaPeriodo(LocalDate inicio, LocalDate fim) {
        return agendamentoRepository.findByDataBetweenOrderByDataAscHoraInicioAsc(inicio, fim);
    }

    @Transactional(readOnly = true)
    public List<HistoricoStatusAgendamento> listarHistoricoStatus(Long agendamentoId) {
        return historicoRepository.findByAgendamentoIdOrderByDataHoraAsc(agendamentoId);
    }
}

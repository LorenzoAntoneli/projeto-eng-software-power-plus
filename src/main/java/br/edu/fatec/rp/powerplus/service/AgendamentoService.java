package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.*;
import br.edu.fatec.rp.powerplus.repository.AgendamentoRepository;
import br.edu.fatec.rp.powerplus.repository.BloqueioAgendaRepository;
import br.edu.fatec.rp.powerplus.repository.DisponibilidadeSemanalRepository;
import br.edu.fatec.rp.powerplus.repository.HistoricoStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Centraliza toda a lógica e as regras de negócio de agendamento de visitas elétricas.
 */
@Service
public class AgendamentoService {

    private static final long ANTECEDENCIA_MINIMA_MINUTOS = 120L; // 2 horas
    private static final DateTimeFormatter FMT_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AgendamentoRepository agendamentoRepository;
    private final DisponibilidadeSemanalRepository disponibilidadeRepository;
    private final BloqueioAgendaRepository bloqueioRepository;
    private final HistoricoStatusRepository historicoRepository;
    private final ClienteService clienteService;
    private final NotificacaoService notificacaoService;
    private final AtomicLong sequencialProtocolo = new AtomicLong(100);

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              DisponibilidadeSemanalRepository disponibilidadeRepository,
                              BloqueioAgendaRepository bloqueioRepository,
                              HistoricoStatusRepository historicoRepository,
                              ClienteService clienteService,
                              NotificacaoService notificacaoService) {
        this.agendamentoRepository = agendamentoRepository;
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.historicoRepository = historicoRepository;
        this.clienteService = clienteService;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public Agendamento agendarAutoatendimento(String nomeCliente, String telefone, String email,
                                              String endereco, PerfilCliente perfil,
                                              LocalDate data, LocalTime horaInicio,
                                              TipoServico tipoServico, String descricao) {
        if (tipoServico == null) {
            throw new RegraNegocioException("O tipo de serviço elétrico é obrigatório.");
        }
        LocalTime horaFim = validarHorarioDisponivelECalcularFim(data, horaInicio, null);
        Cliente cliente = clienteService.obterOuCadastrarNoAgendamento(nomeCliente, telefone, email, endereco, perfil);

        LocalDateTime agora = LocalDateTime.now();
        String protocolo = gerarProximoProtocolo(data.getYear());
        String descLimpa = (descricao != null && !descricao.isBlank()) ? descricao.trim() : null;

        Agendamento novo = new Agendamento(protocolo, cliente, data, horaInicio, horaFim, tipoServico, endereco.trim(), descLimpa);
        novo.setStatus(StatusAgendamento.AGUARDANDO_APROVACAO);
        novo.setCriadoEm(agora);
        novo.setAtualizadoEm(agora);
        Agendamento salvo = agendamentoRepository.save(novo);

        registrarHistorico(salvo, null, StatusAgendamento.AGUARDANDO_APROVACAO,
                "CLIENTE (Autoatendimento)", "Solicitação registrada — aguardando aprovação do proprietário", agora);
        notificacaoService.notificarSolicitacaoRecebida(salvo);
        return salvo;
    }

    @Transactional
    public Agendamento remarcarPeloCliente(String protocolo, LocalDate novaData, LocalTime novaHoraInicio, String motivo) {
        Agendamento agendamento = buscarPorProtocolo(protocolo);
        if (agendamento.getStatus() != StatusAgendamento.CONFIRMADO
                && agendamento.getStatus() != StatusAgendamento.AGUARDANDO_APROVACAO) {
            throw new RegraNegocioException("Apenas solicitações pendentes ou agendamentos confirmados podem ser remarcados.");
        }

        validarAntecedenciaAutoatendimento(agendamento);
        LocalTime novaHoraFim = validarHorarioDisponivelECalcularFim(novaData, novaHoraInicio, agendamento.getId());

        String horarioAnterior = agendamento.getDataHoraInicio().format(FMT_DATA_HORA);
        LocalDateTime agora = LocalDateTime.now();
        StatusAgendamento statusAnterior = agendamento.getStatus();

        agendamento.setData(novaData);
        agendamento.setHoraInicio(novaHoraInicio);
        agendamento.setHoraFim(novaHoraFim);
        agendamento.setStatus(StatusAgendamento.AGUARDANDO_APROVACAO);
        agendamento.setAtualizadoEm(agora);
        Agendamento salvo = agendamentoRepository.save(agendamento);

        String detalhe = "Remarcado de " + horarioAnterior
                + (motivo != null && !motivo.isBlank() ? " (" + motivo.trim() + ")" : "") + " — aguardando aprovação";
        registrarHistorico(salvo, statusAnterior, StatusAgendamento.AGUARDANDO_APROVACAO, "CLIENTE", detalhe, agora);
        notificacaoService.notificarAgendamentoRemarcado(salvo, horarioAnterior);
        return salvo;
    }

    @Transactional
    public Agendamento cancelarPeloCliente(String protocolo, String motivo) {
        Agendamento agendamento = buscarPorProtocolo(protocolo);
        validarTransicaoStatus(agendamento, StatusAgendamento.CANCELADO);
        validarAntecedenciaAutoatendimento(agendamento);

        LocalDateTime agora = LocalDateTime.now();
        StatusAgendamento anterior = agendamento.getStatus();
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamento.setAtualizadoEm(agora);
        Agendamento salvo = agendamentoRepository.save(agendamento);

        registrarHistorico(salvo, anterior, StatusAgendamento.CANCELADO, "CLIENTE",
                (motivo != null && !motivo.isBlank()) ? motivo.trim() : "Cancelado pelo cliente", agora);
        notificacaoService.notificarAgendamentoCancelado(salvo, "CLIENTE");
        return salvo;
    }

    @Transactional
    public Agendamento atualizarStatusOperacional(Long agendamentoId, StatusAgendamento novoStatus,
                                                  String responsavel, String observacao) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Agendamento não encontrado: " + agendamentoId));

        validarTransicaoStatus(agendamento, novoStatus);

        LocalDateTime agora = LocalDateTime.now();
        StatusAgendamento anterior = agendamento.getStatus();
        agendamento.setStatus(novoStatus);
        String obsLimpa = (observacao != null && !observacao.isBlank()) ? observacao.trim() : null;
        if (obsLimpa != null) {
            agendamento.setObservacaoTecnica(obsLimpa);
        }
        agendamento.setAtualizadoEm(agora);
        Agendamento salvo = agendamentoRepository.save(agendamento);

        registrarHistorico(salvo, anterior, novoStatus,
                (responsavel != null && !responsavel.isBlank()) ? responsavel : "PROPRIETARIO", obsLimpa, agora);

        if (anterior == StatusAgendamento.AGUARDANDO_APROVACAO && novoStatus == StatusAgendamento.CONFIRMADO) {
            notificacaoService.notificarAgendamentoConfirmado(salvo);
        } else if (novoStatus == StatusAgendamento.RECUSADO) {
            notificacaoService.notificarAgendamentoRecusado(salvo, obsLimpa != null ? obsLimpa : "Solicitação recusada pelo proprietário");
        } else if (novoStatus == StatusAgendamento.CANCELADO) {
            notificacaoService.notificarAgendamentoCancelado(salvo, "PROPRIETARIO");
        }
        return salvo;
    }

    @Transactional
    public Agendamento aprovarSolicitacao(Long agendamentoId, String responsavel, String observacao) {
        String obs = (observacao != null && !observacao.isBlank()) ? observacao.trim() : "Visita aprovada pelo proprietário";
        return atualizarStatusOperacional(agendamentoId, StatusAgendamento.CONFIRMADO, responsavel, obs);
    }

    @Transactional
    public Agendamento recusarSolicitacao(Long agendamentoId, String responsavel, String motivo) {
        String obs = (motivo != null && !motivo.isBlank()) ? motivo.trim() : "Horário indisponível para atendimento";
        return atualizarStatusOperacional(agendamentoId, StatusAgendamento.RECUSADO, responsavel, obs);
    }

    @Transactional
    public void dispararLembreteManual(Long agendamentoId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Agendamento não encontrado: " + agendamentoId));
        if (agendamento.getStatus() != StatusAgendamento.CONFIRMADO) {
            throw new RegraNegocioException("Lembretes só podem ser enviados para agendamentos confirmados.");
        }
        notificacaoService.enviarLembreteVisita(agendamento);
    }

    @Transactional(readOnly = true)
    public Agendamento buscarPorProtocolo(String protocolo) {
        if (protocolo == null || protocolo.isBlank()) {
            throw new RegraNegocioException("Informe o código de protocolo.");
        }
        return agendamentoRepository.findByProtocoloIgnoreCase(protocolo.trim())
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Protocolo não encontrado: " + protocolo));
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
    public List<Agendamento> listarTodosOrdenados(LocalDate referencia) {
        List<Agendamento> todos = agendamentoRepository.findAllByOrderByDataAscHoraInicioAsc();
        if (referencia == null) {
            return todos;
        }
        List<Agendamento> resultado = new ArrayList<>(todos.size());
        todos.stream().filter(a -> !a.getData().isBefore(referencia)).forEach(resultado::add);
        todos.stream().filter(a -> a.getData().isBefore(referencia))
                .sorted(Comparator.comparing(Agendamento::getData).thenComparing(Agendamento::getHoraInicio).reversed())
                .forEach(resultado::add);
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<Agendamento> listarAgendaPeriodo(LocalDate inicio, LocalDate fim) {
        return agendamentoRepository.findByDataBetweenOrderByDataAscHoraInicioAsc(inicio, fim);
    }

    @Transactional(readOnly = true)
    public List<HistoricoStatusAgendamento> listarHistoricoStatus(Long agendamentoId) {
        return historicoRepository.findByAgendamentoIdOrderByDataHoraAsc(agendamentoId);
    }

    @Transactional(readOnly = true)
    public List<Agendamento> listarPendentesAprovacao() {
        return agendamentoRepository.findByStatusOrderByDataAscHoraInicioAsc(StatusAgendamento.AGUARDANDO_APROVACAO);
    }

    // === Regras internas de validação ===

    private LocalTime validarHorarioDisponivelECalcularFim(LocalDate data, LocalTime horaInicio, Long ignorarId) {
        if (data == null || horaInicio == null) {
            throw new RegraNegocioException("A data e o horário de início são obrigatórios.");
        }
        if (!LocalDateTime.of(data, horaInicio).isAfter(LocalDateTime.now())) {
            throw new RegraNegocioException("O agendamento deve ser realizado para uma data e horário futuros.");
        }
        if (bloqueioRepository.existsByData(data)) {
            throw new RegraNegocioException("A data selecionada encontra-se bloqueada na agenda (folga ou feriado).");
        }

        DisponibilidadeSemanal disp = disponibilidadeRepository.findByDiaSemana(data.getDayOfWeek())
                .filter(DisponibilidadeSemanal::isAtivo)
                .orElseThrow(() -> new RegraNegocioException("Não há expediente configurado para o dia da semana selecionado."));

        LocalTime horaFim = horaInicio.plusMinutes(disp.getDuracaoSlotMinutos());
        if (horaInicio.isBefore(disp.getHoraInicio()) || horaFim.isAfter(disp.getHoraFim())) {
            throw new RegraNegocioException("O horário escolhido está fora da janela de atendimento ("
                    + disp.getHoraInicio() + " às " + disp.getHoraFim() + ").");
        }

        boolean horarioOcupado = agendamentoRepository.findByDataOrderByHoraInicioAsc(data).stream()
                .anyMatch(a -> a.getStatus().ocupaHorarioNaAgenda()
                        && a.getHoraInicio().equals(horaInicio)
                        && !Objects.equals(a.getId(), ignorarId));
        if (horarioOcupado) {
            throw RegraNegocioException.conflito("O horário selecionado já está ocupado por outro atendimento.");
        }
        return horaFim;
    }

    private void validarAntecedenciaAutoatendimento(Agendamento agendamento) {
        if (Duration.between(LocalDateTime.now(), agendamento.getDataHoraInicio()).toMinutes() < ANTECEDENCIA_MINIMA_MINUTOS) {
            throw new RegraNegocioException("Alterações ou cancelamentos pelo autoatendimento exigem no mínimo 2 horas de antecedência. Por favor, fale com o proprietário pelo chat.");
        }
    }

    private void validarTransicaoStatus(Agendamento agendamento, StatusAgendamento novoStatus) {
        if (!agendamento.getStatus().podeTransitarPara(novoStatus)) {
            throw new RegraNegocioException(String.format(
                    "Transição inválida de status: não é permitido alterar de %s para %s.",
                    agendamento.getStatus().getDescricao(),
                    novoStatus != null ? novoStatus.getDescricao() : "NULO"));
        }
    }

    private String gerarProximoProtocolo(int ano) {
        long numero = Math.max(sequencialProtocolo.incrementAndGet(), agendamentoRepository.count() + 101);
        String protocolo = String.format("PP-%d-%04d", ano, numero);
        while (agendamentoRepository.findByProtocoloIgnoreCase(protocolo).isPresent()) {
            numero++;
            protocolo = String.format("PP-%d-%04d", ano, numero);
        }
        return protocolo;
    }

    private void registrarHistorico(Agendamento ag, StatusAgendamento ant, StatusAgendamento novo,
                                    String responsavel, String obs, LocalDateTime quando) {
        historicoRepository.save(new HistoricoStatusAgendamento(ag, ant, novo, responsavel, obs, quando));
    }
}

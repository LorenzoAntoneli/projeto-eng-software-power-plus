package br.edu.fatec.rp.powerplus.web;

import br.edu.fatec.rp.powerplus.domain.Agendamento;
import br.edu.fatec.rp.powerplus.domain.Cliente;
import br.edu.fatec.rp.powerplus.domain.StatusAgendamento;
import br.edu.fatec.rp.powerplus.service.*;
import br.edu.fatec.rp.powerplus.service.notification.CentralNotificacaoService;
import br.edu.fatec.rp.powerplus.web.dto.ApiDtos.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class ProprietarioAdminController {

    private final AutenticacaoProprietarioService authService;
    private final AgendamentoService agendamentoService;
    private final ClienteService clienteService;
    private final DisponibilidadeService disponibilidadeService;
    private final ChatAssistenteService chatService;
    private final CentralNotificacaoService notificacaoService;
    private final Clock clock;

    public ProprietarioAdminController(AutenticacaoProprietarioService authService,
                                       AgendamentoService agendamentoService,
                                       ClienteService clienteService,
                                       DisponibilidadeService disponibilidadeService,
                                       ChatAssistenteService chatService,
                                       CentralNotificacaoService notificacaoService,
                                       Clock clock) {
        this.authService = authService;
        this.agendamentoService = agendamentoService;
        this.clienteService = clienteService;
        this.disponibilidadeService = disponibilidadeService;
        this.chatService = chatService;
        this.notificacaoService = notificacaoService;
        this.clock = clock;
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        var sessao = authService.autenticar(req.username(), req.password());
        return new LoginResponse(sessao.token(), sessao.username(), sessao.nomeExibicao());
    }

    @PostMapping("/auth/logout")
    public Map<String, String> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.encerrarSessao(authHeader);
        return Map.of("mensagem", "Sessão encerrada com sucesso.");
    }

    @GetMapping("/dashboard")
    public DashboardResumoResponse obterDashboard(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        authService.validarToken(authHeader);
        LocalDate hoje = data != null ? data : LocalDate.now(clock);
        LocalDate inicioSemana = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fimSemana = inicioSemana.plusDays(6);

        List<Agendamento> doDia = agendamentoService.listarAgendaDia(hoje);
        List<Agendamento> daSemana = agendamentoService.listarAgendaPeriodo(inicioSemana, fimSemana);
        List<Agendamento> pendentes = agendamentoService.listarPendentesAprovacao();
        List<Agendamento> todos = agendamentoService.listarTodosOrdenados(hoje);

        long aguardandoHoje = doDia.stream().filter(a -> a.getStatus() == StatusAgendamento.AGUARDANDO_APROVACAO).count();
        long confirmados = doDia.stream().filter(a -> a.getStatus() == StatusAgendamento.CONFIRMADO).count();
        long emAtendimento = doDia.stream().filter(a -> a.getStatus() == StatusAgendamento.EM_ATENDIMENTO).count();
        long concluidos = doDia.stream().filter(a -> a.getStatus() == StatusAgendamento.CONCLUIDO).count();
        long cancelados = doDia.stream().filter(a -> a.getStatus() == StatusAgendamento.CANCELADO
                || a.getStatus() == StatusAgendamento.RECUSADO).count();

        return new DashboardResumoResponse(
                hoje,
                doDia.size(),
                aguardandoHoje,
                confirmados,
                emAtendimento,
                concluidos,
                cancelados,
                daSemana.stream().filter(a -> a.getStatus().ocupaHorarioNaAgenda()
                        || a.getStatus() == StatusAgendamento.CONCLUIDO).count(),
                pendentes.size(),
                chatService.contarAguardandoHumano(),
                notificacaoService.contarNaoLidasProprietario(),
                notificacaoService.isIntegracaoWhatsAppAtiva(),
                pendentes.stream().map(AgendamentoResponse::from).toList(),
                doDia.stream().map(AgendamentoResponse::from).toList(),
                todos.stream().map(AgendamentoResponse::from).toList()
        );
    }

    @GetMapping("/agenda/dia")
    public List<AgendamentoResponse> listarAgendaDia(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        authService.validarToken(authHeader);
        return agendamentoService.listarAgendaDia(data).stream().map(AgendamentoResponse::from).toList();
    }

    @GetMapping("/agenda/semana")
    public Map<String, Object> listarAgendaSemana(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataReferencia) {
        authService.validarToken(authHeader);
        LocalDate inicio = dataReferencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fim = inicio.plusDays(6);
        List<AgendamentoResponse> itens = agendamentoService.listarAgendaPeriodo(inicio, fim)
                .stream()
                .map(AgendamentoResponse::from)
                .toList();
        return Map.of(
                "inicioSemana", inicio,
                "fimSemana", fim,
                "agendamentos", itens
        );
    }

    @GetMapping("/agendamentos/{id}/historico")
    public List<HistoricoStatusResponse> listarHistoricoAgendamento(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        authService.validarToken(authHeader);
        return agendamentoService.listarHistoricoStatus(id).stream().map(HistoricoStatusResponse::from).toList();
    }

    @PatchMapping("/agendamentos/{id}/status")
    public AgendamentoResponse atualizarStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody AtualizarStatusRequest req) {
        var sessao = authService.validarToken(authHeader);
        Agendamento salvo = agendamentoService.atualizarStatusOperacional(
                id, req.novoStatus(), sessao.nomeExibicao(), req.observacao()
        );
        return AgendamentoResponse.from(salvo);
    }

    @PostMapping("/agendamentos/{id}/aprovar")
    public AgendamentoResponse aprovarSolicitacao(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody(required = false) DecisaoAprovacaoRequest req) {
        var sessao = authService.validarToken(authHeader);
        Agendamento salvo = agendamentoService.aprovarSolicitacao(
                id, sessao.nomeExibicao(), req != null ? req.observacao() : null
        );
        return AgendamentoResponse.from(salvo);
    }

    @PostMapping("/agendamentos/{id}/recusar")
    public AgendamentoResponse recusarSolicitacao(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody(required = false) DecisaoAprovacaoRequest req) {
        var sessao = authService.validarToken(authHeader);
        Agendamento salvo = agendamentoService.recusarSolicitacao(
                id, sessao.nomeExibicao(), req != null ? req.observacao() : null
        );
        return AgendamentoResponse.from(salvo);
    }

    @PostMapping("/agendamentos/{id}/lembrete")
    public Map<String, String> dispararLembrete(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        authService.validarToken(authHeader);
        agendamentoService.dispararLembreteManual(id);
        return Map.of("mensagem", "Lembrete de visita registrado e enviado com sucesso.");
    }

    @GetMapping("/clientes")
    public List<ClienteResponse> listarClientes(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String termo) {
        authService.validarToken(authHeader);
        return clienteService.listarOuPesquisar(termo).stream().map(ClienteResponse::from).toList();
    }

    @PostMapping("/clientes")
    public ResponseEntity<ClienteResponse> cadastrarCliente(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody SalvarClienteRequest req) {
        authService.validarToken(authHeader);
        Cliente criado = clienteService.cadastrarManual(
                req.nome(), req.telefone(), req.email(), req.endereco(), req.perfil(), req.observacoes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ClienteResponse.from(criado));
    }

    @PutMapping("/clientes/{id}")
    public ClienteResponse atualizarCliente(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody SalvarClienteRequest req) {
        authService.validarToken(authHeader);
        return ClienteResponse.from(clienteService.atualizar(
                id, req.nome(), req.telefone(), req.email(), req.endereco(), req.perfil(), req.observacoes()
        ));
    }

    @GetMapping("/clientes/{id}/historico")
    public List<AgendamentoResponse> historicoCliente(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        authService.validarToken(authHeader);
        clienteService.buscarPorId(id);
        return agendamentoService.consultarHistoricoClienteId(id).stream().map(AgendamentoResponse::from).toList();
    }

    @GetMapping("/disponibilidade")
    public ConfiguracaoAgendaResponse listarDisponibilidade(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.validarToken(authHeader);
        return new ConfiguracaoAgendaResponse(
                disponibilidadeService.listarDisponibilidadeSemanal().stream().map(DisponibilidadeResponse::from).toList(),
                disponibilidadeService.listarBloqueios().stream().map(BloqueioResponse::from).toList()
        );
    }

    @PutMapping("/disponibilidade")
    public DisponibilidadeResponse salvarDisponibilidade(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody SalvarDisponibilidadeRequest req) {
        authService.validarToken(authHeader);
        return DisponibilidadeResponse.from(disponibilidadeService.salvarDisponibilidadeDia(
                req.diaSemana(), req.ativo(), req.horaInicio(), req.horaFim(), req.duracaoSlotMinutos()
        ));
    }

    @PostMapping("/bloqueios")
    public ResponseEntity<Map<String, Object>> criarBloqueio(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody CriarBloqueioRequest req) {
        authService.validarToken(authHeader);
        var res = disponibilidadeService.registrarBloqueioData(req.data(), req.motivo(), req.forcarComConflito());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "bloqueio", BloqueioResponse.from(res.bloqueio()),
                "agendamentosConflitantes", res.agendamentosConflitantes(),
                "protocolosImpactados", res.protocolosImpactados()
        ));
    }

    @DeleteMapping("/bloqueios/{id}")
    public ResponseEntity<Void> removerBloqueio(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        authService.validarToken(authHeader);
        disponibilidadeService.removerBloqueio(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/conversas")
    public List<ConversaChatResponse> listarConversas(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.validarToken(authHeader);
        return chatService.listarTodasConversas().stream().map(ConversaChatResponse::from).toList();
    }

    @PostMapping("/conversas/{id}/responder")
    public ConversaChatResponse responderConversa(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody RespostaProprietarioChatRequest req) {
        authService.validarToken(authHeader);
        return ConversaChatResponse.from(chatService.responderComoProprietario(id, req.resposta(), req.encerrar()));
    }

    @GetMapping("/notificacoes")
    public List<NotificacaoResponse> listarNotificacoes(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.validarToken(authHeader);
        return notificacaoService.listarRecentes().stream().map(NotificacaoResponse::from).toList();
    }

    @PostMapping("/notificacoes/marcar-lidas")
    public Map<String, String> marcarNotificacoesLidas(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.validarToken(authHeader);
        notificacaoService.marcarTodasComoLidasProprietario();
        return Map.of("mensagem", "Notificações marcadas como lidas.");
    }
}

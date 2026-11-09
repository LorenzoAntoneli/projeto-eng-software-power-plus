package br.edu.fatec.rp.powerplus.web;

import br.edu.fatec.rp.powerplus.domain.Agendamento;
import br.edu.fatec.rp.powerplus.domain.Cliente;
import br.edu.fatec.rp.powerplus.domain.StatusAgendamento;
import br.edu.fatec.rp.powerplus.service.*;
import br.edu.fatec.rp.powerplus.web.dto.ApiDtos.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

/**
 * Endpoints do painel interno do eletricista (/api/admin/**).
 * A validação do token de login é feita automaticamente pelo WebConfig antes de entrar nos métodos.
 */
@RestController
@RequestMapping("/api/admin")
public class ProprietarioAdminController {

    private final AutenticacaoService authService;
    private final AgendamentoService agendamentoService;
    private final ClienteService clienteService;
    private final DisponibilidadeService disponibilidadeService;
    private final ChatAssistenteService chatService;
    private final NotificacaoService notificacaoService;

    public ProprietarioAdminController(AutenticacaoService authService,
                                       AgendamentoService agendamentoService,
                                       ClienteService clienteService,
                                       DisponibilidadeService disponibilidadeService,
                                       ChatAssistenteService chatService,
                                       NotificacaoService notificacaoService) {
        this.authService = authService;
        this.agendamentoService = agendamentoService;
        this.clienteService = clienteService;
        this.disponibilidadeService = disponibilidadeService;
        this.chatService = chatService;
        this.notificacaoService = notificacaoService;
    }

    // === 1. Login e Logout ===

    @PostMapping("/auth/login")
    public Map<String, String> login(@Valid @RequestBody LoginRequest req) {
        var sessao = authService.autenticar(req.username(), req.password());
        return Map.of("token", sessao.token(), "username", sessao.username(), "nomeExibicao", sessao.nomeExibicao());
    }

    @PostMapping("/auth/logout")
    public Map<String, String> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.encerrarSessao(authHeader);
        return Map.of("mensagem", "Sessão encerrada com sucesso.");
    }

    // === 2. Resumo do Painel e Visualização da Agenda ===

    @GetMapping("/dashboard")
    public DashboardResumoResponse obterDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        LocalDate hoje = data != null ? data : LocalDate.now();
        LocalDate inicioSemana = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<Agendamento> doDia = agendamentoService.listarAgendaDia(hoje);
        List<Agendamento> daSemana = agendamentoService.listarAgendaPeriodo(inicioSemana, inicioSemana.plusDays(6));
        List<Agendamento> pendentes = agendamentoService.listarPendentesAprovacao();
        List<Agendamento> todos = agendamentoService.listarTodosOrdenados(hoje);

        long aguardandoHoje = contarPorStatus(doDia, StatusAgendamento.AGUARDANDO_APROVACAO);
        long confirmados = contarPorStatus(doDia, StatusAgendamento.CONFIRMADO);
        long emAtendimento = contarPorStatus(doDia, StatusAgendamento.EM_ATENDIMENTO);
        long concluidos = contarPorStatus(doDia, StatusAgendamento.CONCLUIDO);
        long cancelados = contarPorStatus(doDia, StatusAgendamento.CANCELADO) + contarPorStatus(doDia, StatusAgendamento.RECUSADO);
        long totalSemana = daSemana.stream()
                .filter(a -> a.getStatus().ocupaHorarioNaAgenda() || a.getStatus() == StatusAgendamento.CONCLUIDO).count();

        return new DashboardResumoResponse(
                hoje, doDia.size(), aguardandoHoje, confirmados, emAtendimento, concluidos, cancelados,
                totalSemana, pendentes.size(), chatService.contarAguardandoHumano(),
                notificacaoService.contarNaoLidasProprietario(), notificacaoService.isIntegracaoWhatsAppAtiva(),
                pendentes.stream().map(AgendamentoResponse::from).toList(),
                doDia.stream().map(AgendamentoResponse::from).toList(),
                todos.stream().map(AgendamentoResponse::from).toList()
        );
    }

    @GetMapping("/agenda/dia")
    public List<AgendamentoResponse> listarAgendaDia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return agendamentoService.listarAgendaDia(data).stream().map(AgendamentoResponse::from).toList();
    }

    @GetMapping("/agenda/semana")
    public Map<String, Object> listarAgendaSemana(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataReferencia) {
        LocalDate inicio = dataReferencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fim = inicio.plusDays(6);
        List<AgendamentoResponse> itens = agendamentoService.listarAgendaPeriodo(inicio, fim).stream()
                .map(AgendamentoResponse::from).toList();
        return Map.of("inicioSemana", inicio, "fimSemana", fim, "agendamentos", itens);
    }

    // === 3. Aprovação, Recusa e Mudança de Status ===

    @GetMapping("/agendamentos/{id}/historico")
    public List<HistoricoStatusResponse> listarHistoricoAgendamento(@PathVariable Long id) {
        return agendamentoService.listarHistoricoStatus(id).stream().map(HistoricoStatusResponse::from).toList();
    }

    @PatchMapping("/agendamentos/{id}/status")
    public AgendamentoResponse atualizarStatus(@PathVariable Long id,
                                               @RequestAttribute("responsavel") String responsavel,
                                               @Valid @RequestBody AtualizarStatusRequest req) {
        return AgendamentoResponse.from(agendamentoService.atualizarStatusOperacional(
                id, req.novoStatus(), responsavel, req.observacao()));
    }

    @PostMapping("/agendamentos/{id}/aprovar")
    public AgendamentoResponse aprovarSolicitacao(@PathVariable Long id,
                                                  @RequestAttribute("responsavel") String responsavel,
                                                  @RequestBody(required = false) Map<String, String> body) {
        String obs = body != null ? body.get("observacao") : null;
        return AgendamentoResponse.from(agendamentoService.aprovarSolicitacao(id, responsavel, obs));
    }

    @PostMapping("/agendamentos/{id}/recusar")
    public AgendamentoResponse recusarSolicitacao(@PathVariable Long id,
                                                  @RequestAttribute("responsavel") String responsavel,
                                                  @RequestBody(required = false) Map<String, String> body) {
        String obs = body != null ? body.get("observacao") : null;
        return AgendamentoResponse.from(agendamentoService.recusarSolicitacao(id, responsavel, obs));
    }

    @PostMapping("/agendamentos/{id}/lembrete")
    public Map<String, String> dispararLembrete(@PathVariable Long id) {
        agendamentoService.dispararLembreteManual(id);
        return Map.of("mensagem", "Lembrete de visita registrado e enviado com sucesso.");
    }

    // === 4. Cadastro e Histórico de Clientes ===

    @GetMapping("/clientes")
    public List<ClienteResponse> listarClientes(@RequestParam(required = false) String termo) {
        return clienteService.listarOuPesquisar(termo).stream().map(ClienteResponse::from).toList();
    }

    @PostMapping("/clientes")
    public ResponseEntity<ClienteResponse> cadastrarCliente(@Valid @RequestBody SalvarClienteRequest req) {
        Cliente criado = clienteService.cadastrarManual(
                req.nome(), req.telefone(), req.email(), req.endereco(), req.perfil(), req.observacoes());
        return ResponseEntity.status(HttpStatus.CREATED).body(ClienteResponse.from(criado));
    }

    @PutMapping("/clientes/{id}")
    public ClienteResponse atualizarCliente(@PathVariable Long id, @Valid @RequestBody SalvarClienteRequest req) {
        return ClienteResponse.from(clienteService.atualizar(
                id, req.nome(), req.telefone(), req.email(), req.endereco(), req.perfil(), req.observacoes()));
    }

    @GetMapping("/clientes/{id}/historico")
    public List<AgendamentoResponse> historicoCliente(@PathVariable Long id) {
        clienteService.buscarPorId(id);
        return agendamentoService.consultarHistoricoClienteId(id).stream().map(AgendamentoResponse::from).toList();
    }

    // === 5. Expediente Semanal e Bloqueios de Folga/Feriado ===

    @GetMapping("/disponibilidade")
    public Map<String, Object> listarDisponibilidade() {
        return Map.of(
                "disponibilidades", disponibilidadeService.listarDisponibilidadeSemanal().stream().map(DisponibilidadeResponse::from).toList(),
                "bloqueios", disponibilidadeService.listarBloqueios().stream().map(BloqueioResponse::from).toList()
        );
    }

    @PutMapping("/disponibilidade")
    public DisponibilidadeResponse salvarDisponibilidade(@Valid @RequestBody SalvarDisponibilidadeRequest req) {
        return DisponibilidadeResponse.from(disponibilidadeService.salvarDisponibilidadeDia(
                req.diaSemana(), req.ativo(), req.horaInicio(), req.horaFim(), req.duracaoSlotMinutos()));
    }

    @PostMapping("/bloqueios")
    public ResponseEntity<Map<String, Object>> criarBloqueio(@Valid @RequestBody CriarBloqueioRequest req) {
        var res = disponibilidadeService.registrarBloqueioData(req.data(), req.motivo(), req.forcarComConflito());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "bloqueio", BloqueioResponse.from(res.bloqueio()),
                "agendamentosConflitantes", res.agendamentosConflitantes(),
                "protocolosImpactados", res.protocolosImpactados()
        ));
    }

    @DeleteMapping("/bloqueios/{id}")
    public ResponseEntity<Void> removerBloqueio(@PathVariable Long id) {
        disponibilidadeService.removerBloqueio(id);
        return ResponseEntity.noContent().build();
    }

    // === 6. Chat com Clientes e Central de Avisos ===

    @GetMapping("/conversas")
    public List<ConversaChatResponse> listarConversas() {
        return chatService.listarTodasConversas().stream().map(ConversaChatResponse::from).toList();
    }

    @PostMapping("/conversas/{id}/responder")
    public ConversaChatResponse responderConversa(@PathVariable Long id,
                                                  @Valid @RequestBody RespostaProprietarioChatRequest req) {
        return ConversaChatResponse.from(chatService.responderComoProprietario(id, req.resposta(), req.encerrar()));
    }

    @GetMapping("/notificacoes")
    public List<NotificacaoResponse> listarNotificacoes() {
        return notificacaoService.listarRecentes().stream().map(NotificacaoResponse::from).toList();
    }

    @PostMapping("/notificacoes/marcar-lidas")
    public Map<String, String> marcarNotificacoesLidas() {
        notificacaoService.marcarTodasComoLidasProprietario();
        return Map.of("mensagem", "Notificações marcadas como lidas.");
    }

    private long contarPorStatus(List<Agendamento> lista, StatusAgendamento status) {
        return lista.stream().filter(a -> a.getStatus() == status).count();
    }
}

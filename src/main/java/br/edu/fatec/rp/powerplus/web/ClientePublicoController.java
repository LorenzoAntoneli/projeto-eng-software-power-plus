package br.edu.fatec.rp.powerplus.web;

import br.edu.fatec.rp.powerplus.domain.Agendamento;
import br.edu.fatec.rp.powerplus.domain.PerfilCliente;
import br.edu.fatec.rp.powerplus.domain.TipoServico;
import br.edu.fatec.rp.powerplus.service.AgendamentoService;
import br.edu.fatec.rp.powerplus.service.ChatAssistenteService;
import br.edu.fatec.rp.powerplus.service.DisponibilidadeService;
import br.edu.fatec.rp.powerplus.web.dto.ApiDtos.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Endpoints públicos usados pelo portal do cliente (agendar, consultar protocolo e chat).
 */
@RestController
@RequestMapping("/api/publico")
public class ClientePublicoController {

    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final DisponibilidadeService disponibilidadeService;
    private final AgendamentoService agendamentoService;
    private final ChatAssistenteService chatAssistenteService;

    public ClientePublicoController(DisponibilidadeService disponibilidadeService,
                                    AgendamentoService agendamentoService,
                                    ChatAssistenteService chatAssistenteService) {
        this.disponibilidadeService = disponibilidadeService;
        this.agendamentoService = agendamentoService;
        this.chatAssistenteService = chatAssistenteService;
    }

    @GetMapping("/catalogo")
    public Map<String, Object> obterCatalogo() {
        List<OpcaoEnumDto> servicos = Arrays.stream(TipoServico.values())
                .map(s -> new OpcaoEnumDto(s.name(), s.getDescricao(), s.getDuracaoPadraoMinutos())).toList();
        List<OpcaoEnumDto> perfis = Arrays.stream(PerfilCliente.values())
                .map(p -> new OpcaoEnumDto(p.name(), p.getDescricao(), null)).toList();
        return Map.of("tiposServico", servicos, "perfisCliente", perfis);
    }

    @GetMapping("/horarios-livres")
    public Map<String, Object> consultarHorariosLivres(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        List<String> livres = disponibilidadeService.consultarHorariosLivres(data).stream()
                .map(h -> h.format(FMT_HORA)).toList();
        return Map.of("data", data, "diaSemana", data.getDayOfWeek().name(), "horariosLivres", livres);
    }

    @PostMapping("/agendamentos")
    public ResponseEntity<AgendamentoResponse> criarAgendamento(@Valid @RequestBody NovoAgendamentoRequest req) {
        Agendamento criado = agendamentoService.agendarAutoatendimento(
                req.nomeCliente(), req.telefone(), req.email(), req.endereco(),
                req.perfilCliente(), req.data(), req.horaInicio(), req.tipoServico(), req.descricaoServico()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(AgendamentoResponse.from(criado));
    }

    @GetMapping("/agendamentos/protocolo/{protocolo}")
    public Map<String, Object> consultarPorProtocolo(@PathVariable String protocolo) {
        Agendamento a = agendamentoService.buscarPorProtocolo(protocolo);
        List<HistoricoStatusResponse> historico = agendamentoService.listarHistoricoStatus(a.getId()).stream()
                .map(HistoricoStatusResponse::from).toList();
        return Map.of("agendamento", AgendamentoResponse.from(a), "historico", historico);
    }

    @GetMapping("/agendamentos/cliente")
    public List<AgendamentoResponse> consultarPorTelefone(@RequestParam String telefone) {
        return agendamentoService.consultarPorTelefoneCliente(telefone).stream()
                .map(AgendamentoResponse::from).toList();
    }

    @PutMapping("/agendamentos/{protocolo}/remarcar")
    public AgendamentoResponse remarcar(@PathVariable String protocolo,
                                        @Valid @RequestBody RemarcarAgendamentoRequest req) {
        return AgendamentoResponse.from(agendamentoService.remarcarPeloCliente(
                protocolo, req.novaData(), req.novaHoraInicio(), req.motivo()));
    }

    @PostMapping("/agendamentos/{protocolo}/cancelar")
    public AgendamentoResponse cancelar(@PathVariable String protocolo,
                                        @RequestBody(required = false) Map<String, String> body) {
        String motivo = body != null ? body.get("motivo") : null;
        return AgendamentoResponse.from(agendamentoService.cancelarPeloCliente(protocolo, motivo));
    }

    @PostMapping("/chat/mensagens")
    public ConversaChatResponse enviarMensagemChat(@Valid @RequestBody MensagemClienteChatRequest req) {
        return ConversaChatResponse.from(chatAssistenteService.iniciarOuEnviarMensagemCliente(
                req.conversaId(), req.nomeCliente(), req.telefoneCliente(), req.mensagem(), req.solicitarHumano()));
    }

    @GetMapping("/chat/{conversaId}")
    public ConversaChatResponse consultarConversa(@PathVariable Long conversaId) {
        return ConversaChatResponse.from(chatAssistenteService.buscarPorId(conversaId));
    }
}

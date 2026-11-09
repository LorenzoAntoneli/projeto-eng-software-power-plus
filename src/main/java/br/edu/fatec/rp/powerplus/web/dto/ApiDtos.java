package br.edu.fatec.rp.powerplus.web.dto;

import br.edu.fatec.rp.powerplus.domain.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Formatos simples de JSON trocados entre as telas (HTML/JS) e o servidor Java.
 */
public final class ApiDtos {

    private ApiDtos() {}

    // === 1. Dados recebidos do navegador (Requests) ===

    public record NovoAgendamentoRequest(
            @NotBlank(message = "O nome completo é obrigatório.") String nomeCliente,
            @NotBlank(message = "O telefone/WhatsApp com DDD é obrigatório.") String telefone,
            String email,
            @NotBlank(message = "O endereço completo do atendimento é obrigatório.") String endereco,
            PerfilCliente perfilCliente,
            @NotNull(message = "Selecione a data desejada.") LocalDate data,
            @NotNull(message = "Selecione um horário disponível.") LocalTime horaInicio,
            @NotNull(message = "Selecione o tipo de serviço elétrico.") TipoServico tipoServico,
            String descricaoServico
    ) {}

    public record RemarcarAgendamentoRequest(
            @NotNull(message = "Informe a nova data.") LocalDate novaData,
            @NotNull(message = "Informe o novo horário.") LocalTime novaHoraInicio,
            String motivo
    ) {}

    public record AtualizarStatusRequest(
            @NotNull(message = "Informe o novo status.") StatusAgendamento novoStatus,
            String observacao
    ) {}

    public record SalvarClienteRequest(
            @NotBlank(message = "O nome do cliente é obrigatório.") String nome,
            @NotBlank(message = "O telefone é obrigatório.") String telefone,
            String email,
            @NotBlank(message = "O endereço é obrigatório.") String endereco,
            PerfilCliente perfil,
            String observacoes
    ) {}

    public record SalvarDisponibilidadeRequest(
            @NotNull DayOfWeek diaSemana, boolean ativo,
            @NotNull LocalTime horaInicio, @NotNull LocalTime horaFim,
            int duracaoSlotMinutos
    ) {}

    public record CriarBloqueioRequest(
            @NotNull(message = "Informe a data a bloquear.") LocalDate data,
            @NotBlank(message = "Informe o motivo do bloqueio (ex.: feriado, folga).") String motivo,
            boolean forcarComConflito
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Informe o usuário.") String username,
            @NotBlank(message = "Informe a senha.") String password
    ) {}

    public record MensagemClienteChatRequest(
            Long conversaId, String nomeCliente, String telefoneCliente,
            @NotBlank(message = "Digite sua mensagem.") String mensagem,
            boolean solicitarHumano
    ) {}

    public record RespostaProprietarioChatRequest(
            @NotBlank(message = "Digite a resposta para o cliente.") String resposta,
            boolean encerrar
    ) {}

    // === 2. Dados devolvidos pelo servidor (Responses) ===

    public record OpcaoEnumDto(String codigo, String descricao, Integer duracaoMinutos) {}

    public record ClienteResponse(Long id, String nome, String telefone, String email, String enderecoPadrao,
                                  String perfilCodigo, String perfilDescricao, String observacoes, LocalDateTime criadoEm) {
        public static ClienteResponse from(Cliente c) {
            return new ClienteResponse(c.getId(), c.getNome(), c.getTelefone(), c.getEmail(), c.getEnderecoPadrao(),
                    c.getPerfil().name(), c.getPerfil().getDescricao(), c.getObservacoes(), c.getCriadoEm());
        }
    }

    public record AgendamentoResponse(Long id, String protocolo, Long clienteId, String clienteNome, String clienteTelefone,
                                      String clientePerfil, LocalDate data, String horaInicio, String horaFim,
                                      String tipoServicoCodigo, String tipoServicoDescricao, String enderecoAtendimento,
                                      String descricaoServico, String statusCodigo, String statusDescricao,
                                      String observacaoTecnica, LocalDateTime criadoEm) {
        public static AgendamentoResponse from(Agendamento a) {
            return new AgendamentoResponse(a.getId(), a.getProtocolo(), a.getCliente().getId(), a.getCliente().getNome(),
                    a.getCliente().getTelefone(), a.getCliente().getPerfil().getDescricao(), a.getData(),
                    a.getHoraInicio().toString(), a.getHoraFim().toString(), a.getTipoServico().name(),
                    a.getTipoServico().getDescricao(), a.getEnderecoAtendimento(), a.getDescricaoServico(),
                    a.getStatus().name(), a.getStatus().getDescricao(), a.getObservacaoTecnica(), a.getCriadoEm());
        }
    }

    public record HistoricoStatusResponse(Long id, String statusAnterior, String statusNovo,
                                          String responsavel, String observacao, LocalDateTime dataHora) {
        public static HistoricoStatusResponse from(HistoricoStatusAgendamento h) {
            String anterior = h.getStatusAnterior() != null ? h.getStatusAnterior().getDescricao() : "Criação";
            return new HistoricoStatusResponse(h.getId(), anterior, h.getStatusNovo().getDescricao(),
                    h.getResponsavel(), h.getObservacao(), h.getDataHora());
        }
    }

    public record MensagemChatDto(Long id, String remetente, String conteudo, LocalDateTime enviadaEm) {}

    public record ConversaChatResponse(Long id, String nomeCliente, String telefoneCliente, String statusCodigo,
                                       String statusDescricao, LocalDateTime atualizadaEm, List<MensagemChatDto> mensagens) {
        public static ConversaChatResponse from(ConversaChat c) {
            List<MensagemChatDto> msgs = c.getMensagens().stream()
                    .map(m -> new MensagemChatDto(m.getId(), m.getRemetente(), m.getConteudo(), m.getEnviadaEm()))
                    .toList();
            return new ConversaChatResponse(c.getId(), c.getNomeCliente(), c.getTelefoneCliente(),
                    c.getStatus().name(), c.getStatus().getDescricao(), c.getAtualizadaEm(), msgs);
        }
    }

    public record DisponibilidadeResponse(Long id, String diaSemana, boolean ativo,
                                          String horaInicio, String horaFim, int duracaoSlotMinutos) {
        public static DisponibilidadeResponse from(DisponibilidadeSemanal d) {
            return new DisponibilidadeResponse(d.getId(), d.getDiaSemana().name(), d.isAtivo(),
                    d.getHoraInicio().toString(), d.getHoraFim().toString(), d.getDuracaoSlotMinutos());
        }
    }

    public record BloqueioResponse(Long id, LocalDate data, String motivo, LocalDateTime criadoEm) {
        public static BloqueioResponse from(BloqueioAgenda b) {
            return new BloqueioResponse(b.getId(), b.getData(), b.getMotivo(), b.getCriadoEm());
        }
    }

    public record NotificacaoResponse(Long id, String tipo, String destinatarioPapel, String titulo, String mensagem,
                                      String protocoloReferencia, String canalEnvio, boolean lida, LocalDateTime criadaEm) {
        public static NotificacaoResponse from(NotificacaoRegistro n) {
            return new NotificacaoResponse(n.getId(), n.getTipo().name(), n.getDestinatarioPapel(), n.getTitulo(),
                    n.getMensagem(), n.getProtocoloReferencia(), n.getCanalEnvio(), n.isLida(), n.getCriadaEm());
        }
    }

    public record DashboardResumoResponse(
            LocalDate dataReferencia, long totalHoje, long aguardandoAprovacaoHoje, long confirmadosHoje,
            long emAtendimentoHoje, long concluidosHoje, long canceladosHoje, long totalSemana,
            long pendentesAprovacaoTotal, long chatsAguardandoHumano, long notificacoesNaoLidas,
            boolean whatsappFeatureFlagAtiva, List<AgendamentoResponse> pendentesAprovacao,
            List<AgendamentoResponse> atendimentosHoje, List<AgendamentoResponse> todosAgendamentos
    ) {}
}

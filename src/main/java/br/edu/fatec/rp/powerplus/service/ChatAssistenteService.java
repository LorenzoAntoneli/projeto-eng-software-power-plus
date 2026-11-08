package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.ConversaChat;
import br.edu.fatec.rp.powerplus.domain.StatusConversaChat;
import br.edu.fatec.rp.powerplus.domain.exception.RecursoNaoEncontradoException;
import br.edu.fatec.rp.powerplus.domain.exception.RegraNegocioException;
import br.edu.fatec.rp.powerplus.repository.ConversaChatRepository;
import br.edu.fatec.rp.powerplus.service.notification.NotificacaoGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ChatAssistenteService {

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ConversaChatRepository conversaRepository;
    private final DisponibilidadeService disponibilidadeService;
    private final NotificacaoGateway notificacaoGateway;
    private final Clock clock;

    public ChatAssistenteService(ConversaChatRepository conversaRepository,
                                 DisponibilidadeService disponibilidadeService,
                                 NotificacaoGateway notificacaoGateway,
                                 Clock clock) {
        this.conversaRepository = conversaRepository;
        this.disponibilidadeService = disponibilidadeService;
        this.notificacaoGateway = notificacaoGateway;
        this.clock = clock;
    }

    @Transactional
    public ConversaChat iniciarOuEnviarMensagemCliente(Long conversaId, String nomeCliente,
                                                       String telefoneCliente, String mensagem,
                                                       boolean solicitarHumano) {
        if (mensagem == null || mensagem.isBlank()) {
            throw new RegraNegocioException("Digite uma mensagem para o atendimento.");
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        ConversaChat conversa;
        if (conversaId != null) {
            conversa = buscarPorId(conversaId);
        } else {
            String nome = (nomeCliente != null && !nomeCliente.isBlank()) ? nomeCliente.trim() : "Visitante";
            conversa = new ConversaChat(nome, telefoneCliente, agora);
            conversa.adicionarMensagem("ASSISTENTE",
                    "Olá, " + nome + "! Sou o assistente virtual da Power Plus Serviços Elétricos. " +
                    "Posso informar nossos próximos horários livres, tirar dúvidas sobre serviços/orçamentos " +
                    "ou encaminhar sua conversa diretamente ao eletricista.",
                    agora);
        }

        String textoCliente = mensagem.trim();
        conversa.adicionarMensagem("CLIENTE", textoCliente, agora);

        // Se a conversa já está sob atendimento humano, apenas registra a mensagem do cliente
        if (conversa.getStatus() == StatusConversaChat.EM_ATENDIMENTO_HUMANO
                || conversa.getStatus() == StatusConversaChat.AGUARDANDO_HUMANO) {
            ConversaChat salva = conversaRepository.save(conversa);
            notificacaoGateway.notificarTransbordoChat(salva.getId(), salva.getNomeCliente(), textoCliente);
            return salva;
        }

        String normalizado = normalizarTexto(textoCliente);

        if (solicitarHumano || pedeAtendimentoHumano(normalizado)) {
            return encaminharParaHumano(conversa, textoCliente, agora,
                    "Perfeito! Encaminhei todo o histórico desta conversa para o Leonardo (proprietário da Power Plus). " +
                    "Ele responderá por aqui ou pelo seu telefone em instantes.");
        }

        if (contemAlgum(normalizado, "emergencia", "fogo", "faisca", "cheiro de queimado", "choque", "explodiu")) {
            return encaminharParaHumano(conversa, textoCliente, agora,
                    "ATENÇÃO DE SEGURANÇA: desligue imediatamente o disjuntor geral do imóvel e não toque em fios expostos! " +
                    "Já acionei o eletricista com prioridade máxima e encaminhei sua mensagem.");
        }

        if (contemAlgum(normalizado, "horario", "agenda", "disponivel", "vaga", "quando pode", "amanha", "hoje", "marcar")) {
            String resumoHorarios = montarResumoProximosHorarios(agora.toLocalDate());
            conversa.adicionarMensagem("ASSISTENTE", resumoHorarios, agora);
            return conversaRepository.save(conversa);
        }

        if (contemAlgum(normalizado, "valor", "preco", "orcamento", "quanto custa", "taxa", "visita", "pagamento", "pix", "cartao")) {
            conversa.adicionarMensagem("ASSISTENTE",
                    "Trabalhamos com orçamento transparente! A vistoria técnica presencial em Ribeirão Preto avalia a instalação com segurança, " +
                    "e o valor da visita é integralmente abatido caso o serviço seja aprovado. Aceitamos PIX, cartão de crédito/débito e boleto para condomínios/empresas. " +
                    "Deseja escolher um horário livre na aba 'Agendar Visita' ou falar com o proprietário?",
                    agora);
            return conversaRepository.save(conversa);
        }

        if (contemAlgum(normalizado, "servico", "chuveiro", "tomada", "disjuntor", "quadro", "condominio", "empresa", "manutencao", "instalacao")) {
            conversa.adicionarMensagem("ASSISTENTE",
                    "A Power Plus realiza: (1) Reparos elétricos e curtos; (2) Instalação de chuveiros, tomadas 127V/220V e iluminação LED; " +
                    "(3) Montagem e adequação de quadros de disjuntores/DR/DPS; e (4) Manutenção preventiva para empresas e condomínios com histórico registrado.",
                    agora);
            return conversaRepository.save(conversa);
        }

        if (contemAlgum(normalizado, "regiao", "bairro", "onde atende", "ribeirao", "cidade", "garantia", "nota fiscal")) {
            conversa.adicionarMensagem("ASSISTENTE",
                    "Atendemos toda Ribeirão Preto e região metropolitana, com garantia de 90 dias em todos os serviços executados e emissão de comprovante/nota para clientes residenciais, empresas e condomínios.",
                    agora);
            return conversaRepository.save(conversa);
        }

        // UC03 Fluxo Alternativo 2a: assunto específico/complexo -> transbordo automático com histórico
        return encaminharParaHumano(conversa, textoCliente, agora,
                "Sua solicitação envolve detalhes específicos que precisam da avaliação direta do eletricista. " +
                "Encaminhei nossa conversa com todo o histórico para o proprietário assumir o atendimento!");
    }

    @Transactional
    public ConversaChat responderComoProprietario(Long conversaId, String resposta, boolean encerrar) {
        if (resposta == null || resposta.isBlank()) {
            throw new RegraNegocioException("Informe a mensagem de resposta ao cliente.");
        }
        ConversaChat conversa = buscarPorId(conversaId);
        LocalDateTime agora = LocalDateTime.now(clock);
        conversa.adicionarMensagem("PROPRIETARIO", resposta.trim(), agora);
        conversa.setStatus(encerrar ? StatusConversaChat.ENCERRADO : StatusConversaChat.EM_ATENDIMENTO_HUMANO);
        return conversaRepository.save(conversa);
    }

    @Transactional(readOnly = true)
    public ConversaChat buscarPorId(Long conversaId) {
        return conversaRepository.findById(conversaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conversa não encontrada: " + conversaId));
    }

    @Transactional(readOnly = true)
    public List<ConversaChat> listarTodasConversas() {
        return conversaRepository.findAllByOrderByAtualizadaEmDesc();
    }

    @Transactional(readOnly = true)
    public long contarAguardandoHumano() {
        return conversaRepository.countByStatus(StatusConversaChat.AGUARDANDO_HUMANO);
    }

    private ConversaChat encaminharParaHumano(ConversaChat conversa, String textoCliente,
                                              LocalDateTime agora, String mensagemAssistente) {
        conversa.setStatus(StatusConversaChat.AGUARDANDO_HUMANO);
        conversa.adicionarMensagem("ASSISTENTE", mensagemAssistente, agora);
        ConversaChat salva = conversaRepository.save(conversa);
        notificacaoGateway.notificarTransbordoChat(salva.getId(), salva.getNomeCliente(), textoCliente);
        return salva;
    }

    private String montarResumoProximosHorarios(LocalDate dataInicial) {
        StringBuilder sb = new StringBuilder("Consultei a agenda em tempo real! Próximos horários livres: ");
        int diasComVaga = 0;
        for (int i = 0; i < 7 && diasComVaga < 2; i++) {
            LocalDate d = dataInicial.plusDays(i);
            List<LocalTime> livres = disponibilidadeService.consultarHorariosLivres(d);
            if (!livres.isEmpty()) {
                String amostra = livres.stream().limit(4).map(h -> h.format(FMT_HORA)).collect(Collectors.joining(", "));
                if (diasComVaga > 0) {
                    sb.append(" | ");
                }
                sb.append(d.format(FMT_DATA)).append(": ").append(amostra);
                diasComVaga++;
            }
        }
        if (diasComVaga == 0) {
            return "No momento não encontrei vagas nos próximos dias úteis. Deseja que eu encaminhe seu contato ao proprietário para encaixe?";
        }
        sb.append(". Você pode reservar imediatamente clicando em 'Agendar Visita' no topo da tela!");
        return sb.toString();
    }

    private boolean pedeAtendimentoHumano(String normalizado) {
        return contemAlgum(normalizado, "falar com eletricista", "falar com proprietario",
                "falar com humano", "atendente", "pessoa real", "leonardo", "carlos", "ligar");
    }

    private boolean contemAlgum(String texto, String... termos) {
        for (String t : termos) {
            if (texto.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private String normalizarTexto(String entrada) {
        String semAcento = Normalizer.normalize(entrada, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }
}

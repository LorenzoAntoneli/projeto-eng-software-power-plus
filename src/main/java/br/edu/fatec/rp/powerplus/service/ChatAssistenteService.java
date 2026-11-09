package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.ConversaChat;
import br.edu.fatec.rp.powerplus.domain.RegraNegocioException;
import br.edu.fatec.rp.powerplus.domain.StatusConversaChat;
import br.edu.fatec.rp.powerplus.repository.ConversaChatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Assistente virtual de atendimento: responde dúvidas comuns (horários, valores, serviços)
 * e encaminha pedidos complexos ou emergências para o eletricista humano.
 */
@Service
public class ChatAssistenteService {

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ConversaChatRepository conversaRepository;
    private final DisponibilidadeService disponibilidadeService;
    private final NotificacaoService notificacaoService;

    public ChatAssistenteService(ConversaChatRepository conversaRepository,
                                 DisponibilidadeService disponibilidadeService,
                                 NotificacaoService notificacaoService) {
        this.conversaRepository = conversaRepository;
        this.disponibilidadeService = disponibilidadeService;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public ConversaChat iniciarOuEnviarMensagemCliente(Long conversaId, String nomeCliente,
                                                       String telefoneCliente, String mensagem,
                                                       boolean solicitarHumano) {
        if (mensagem == null || mensagem.isBlank()) {
            throw new RegraNegocioException("Digite uma mensagem para o atendimento.");
        }

        LocalDateTime agora = LocalDateTime.now();
        ConversaChat conversa;
        if (conversaId != null) {
            conversa = buscarPorId(conversaId);
        } else {
            String nome = (nomeCliente != null && !nomeCliente.isBlank()) ? nomeCliente.trim() : "Visitante";
            conversa = new ConversaChat(nome, telefoneCliente, agora);
            conversa.adicionarMensagem("ASSISTENTE",
                    "Olá, " + nome + "! Sou o assistente virtual da Power Plus Serviços Elétricos. "
                    + "Posso informar nossos próximos horários livres, tirar dúvidas sobre serviços/orçamentos "
                    + "ou encaminhar sua conversa diretamente ao eletricista.", agora);
        }

        String textoCliente = mensagem.trim();
        conversa.adicionarMensagem("CLIENTE", textoCliente, agora);

        // Se já está com atendimento humano, apenas salva e avisa o proprietário
        if (conversa.getStatus() == StatusConversaChat.EM_ATENDIMENTO_HUMANO
                || conversa.getStatus() == StatusConversaChat.AGUARDANDO_HUMANO) {
            ConversaChat salva = conversaRepository.save(conversa);
            notificacaoService.notificarTransbordoChat(salva.getId(), salva.getNomeCliente(), textoCliente);
            return salva;
        }

        String texto = removerAcentosEMinusculas(textoCliente);

        if (solicitarHumano || contemAlgum(texto, "falar com eletricista", "falar com proprietario",
                "falar com humano", "atendente", "pessoa real", "leonardo", "carlos", "ligar")) {
            return encaminharParaHumano(conversa, textoCliente, agora,
                    "Perfeito! Encaminhei todo o histórico desta conversa para o Leonardo (proprietário da Power Plus). "
                    + "Ele responderá por aqui ou pelo seu telefone em instantes.");
        }

        if (contemAlgum(texto, "emergencia", "fogo", "faisca", "cheiro de queimado", "choque", "explodiu")) {
            return encaminharParaHumano(conversa, textoCliente, agora,
                    "ATENÇÃO DE SEGURANÇA: desligue imediatamente o disjuntor geral do imóvel e não toque em fios expostos! "
                    + "Já acionei o eletricista com prioridade máxima e encaminhei sua mensagem.");
        }

        if (contemAlgum(texto, "horario", "agenda", "disponivel", "vaga", "quando pode", "amanha", "hoje", "marcar")) {
            conversa.adicionarMensagem("ASSISTENTE", montarResumoProximosHorarios(agora.toLocalDate()), agora);
            return conversaRepository.save(conversa);
        }

        if (contemAlgum(texto, "valor", "preco", "orcamento", "quanto custa", "taxa", "visita", "pagamento", "pix", "cartao")) {
            conversa.adicionarMensagem("ASSISTENTE",
                    "Trabalhamos com orçamento transparente! A vistoria técnica presencial em Ribeirão Preto avalia a instalação com segurança, "
                    + "e o valor da visita é integralmente abatido caso o serviço seja aprovado. Aceitamos PIX, cartão de crédito/débito e boleto para condomínios/empresas. "
                    + "Deseja escolher um horário livre na aba 'Agendar Visita' ou falar com o proprietário?", agora);
            return conversaRepository.save(conversa);
        }

        if (contemAlgum(texto, "servico", "chuveiro", "tomada", "disjuntor", "quadro", "condominio", "empresa", "manutencao", "instalacao")) {
            conversa.adicionarMensagem("ASSISTENTE",
                    "A Power Plus realiza: (1) Reparos elétricos e curtos; (2) Instalação de chuveiros, tomadas 127V/220V e iluminação LED; "
                    + "(3) Montagem e adequação de quadros de disjuntores/DR/DPS; e (4) Manutenção preventiva para empresas e condomínios com histórico registrado.", agora);
            return conversaRepository.save(conversa);
        }

        if (contemAlgum(texto, "regiao", "bairro", "onde atende", "ribeirao", "cidade", "garantia", "nota fiscal")) {
            conversa.adicionarMensagem("ASSISTENTE",
                    "Atendemos toda Ribeirão Preto e região metropolitana, com garantia de 90 dias em todos os serviços executados e emissão de comprovante/nota para clientes residenciais, empresas e condomínios.", agora);
            return conversaRepository.save(conversa);
        }

        return encaminharParaHumano(conversa, textoCliente, agora,
                "Sua solicitação envolve detalhes específicos que precisam da avaliação direta do eletricista. "
                + "Encaminhei nossa conversa com todo o histórico para o proprietário assumir o atendimento!");
    }

    @Transactional
    public ConversaChat responderComoProprietario(Long conversaId, String resposta, boolean encerrar) {
        if (resposta == null || resposta.isBlank()) {
            throw new RegraNegocioException("Informe a mensagem de resposta ao cliente.");
        }
        ConversaChat conversa = buscarPorId(conversaId);
        conversa.adicionarMensagem("PROPRIETARIO", resposta.trim(), LocalDateTime.now());
        conversa.setStatus(encerrar ? StatusConversaChat.ENCERRADO : StatusConversaChat.EM_ATENDIMENTO_HUMANO);
        return conversaRepository.save(conversa);
    }

    @Transactional(readOnly = true)
    public ConversaChat buscarPorId(Long conversaId) {
        return conversaRepository.findById(conversaId)
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Conversa não encontrada: " + conversaId));
    }

    @Transactional(readOnly = true)
    public List<ConversaChat> listarTodasConversas() {
        return conversaRepository.findAllByOrderByAtualizadaEmDesc();
    }

    @Transactional(readOnly = true)
    public long contarAguardandoHumano() {
        return conversaRepository.countByStatus(StatusConversaChat.AGUARDANDO_HUMANO);
    }

    private ConversaChat encaminharParaHumano(ConversaChat conversa, String textoCliente, LocalDateTime agora, String respostaAssistente) {
        conversa.setStatus(StatusConversaChat.AGUARDANDO_HUMANO);
        conversa.adicionarMensagem("ASSISTENTE", respostaAssistente, agora);
        ConversaChat salva = conversaRepository.save(conversa);
        notificacaoService.notificarTransbordoChat(salva.getId(), salva.getNomeCliente(), textoCliente);
        return salva;
    }

    private String montarResumoProximosHorarios(LocalDate dataInicial) {
        StringBuilder sb = new StringBuilder("Consultei a agenda em tempo real! Próximos horários livres: ");
        int diasComVaga = 0;
        for (int i = 0; i < 7 && diasComVaga < 2; i++) {
            LocalDate dia = dataInicial.plusDays(i);
            List<LocalTime> livres = disponibilidadeService.consultarHorariosLivres(dia);
            if (!livres.isEmpty()) {
                if (diasComVaga > 0) sb.append(" | ");
                String amostra = livres.stream().limit(4).map(h -> h.format(FMT_HORA)).collect(Collectors.joining(", "));
                sb.append(dia.format(FMT_DATA)).append(": ").append(amostra);
                diasComVaga++;
            }
        }
        return diasComVaga == 0
                ? "No momento não encontrei vagas nos próximos dias úteis. Deseja que eu encaminhe seu contato ao proprietário para encaixe?"
                : sb.append(". Você pode reservar imediatamente clicando em 'Agendar Visita' no topo da tela!").toString();
    }

    private boolean contemAlgum(String texto, String... termos) {
        for (String t : termos) {
            if (texto.contains(t)) return true;
        }
        return false;
    }

    private String removerAcentosEMinusculas(String entrada) {
        return Normalizer.normalize(entrada, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);
    }
}

package br.edu.fatec.rp.powerplus.config;

import br.edu.fatec.rp.powerplus.domain.*;
import br.edu.fatec.rp.powerplus.repository.*;
import br.edu.fatec.rp.powerplus.service.AutenticacaoProprietarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

/**
 * Carga inicial de dados fictícios/anonimizados para demonstração na Banca de Produto
 * (em conformidade com a seção 23 do Edital sobre Ética, Privacidade e LGPD).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final boolean seedEnabled;
    private final AutenticacaoProprietarioService authService;
    private final DisponibilidadeSemanalRepository disponibilidadeRepository;
    private final BloqueioAgendaRepository bloqueioRepository;
    private final ClienteRepository clienteRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final HistoricoStatusRepository historicoRepository;
    private final ConversaChatRepository conversaChatRepository;
    private final NotificacaoRegistroRepository notificacaoRepository;
    private final Clock clock;

    public DataSeeder(@Value("${powerplus.seed.enabled:true}") boolean seedEnabled,
                      AutenticacaoProprietarioService authService,
                      DisponibilidadeSemanalRepository disponibilidadeRepository,
                      BloqueioAgendaRepository bloqueioRepository,
                      ClienteRepository clienteRepository,
                      AgendamentoRepository agendamentoRepository,
                      HistoricoStatusRepository historicoRepository,
                      ConversaChatRepository conversaChatRepository,
                      NotificacaoRegistroRepository notificacaoRepository,
                      Clock clock) {
        this.seedEnabled = seedEnabled;
        this.authService = authService;
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.clienteRepository = clienteRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.historicoRepository = historicoRepository;
        this.conversaChatRepository = conversaChatRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        authService.garantirUsuarioPadrao();
        if (!seedEnabled || disponibilidadeRepository.count() > 0) {
            return;
        }

        // 1. Disponibilidade padrão Seg-Sex (08:00-18:00), Sáb (08:00-13:00), Dom (Inativo)
        for (DayOfWeek dia : DayOfWeek.values()) {
            if (dia == DayOfWeek.SUNDAY) {
                disponibilidadeRepository.save(new DisponibilidadeSemanal(dia, false, LocalTime.of(8, 0), LocalTime.of(12, 0), 60));
            } else if (dia == DayOfWeek.SATURDAY) {
                disponibilidadeRepository.save(new DisponibilidadeSemanal(dia, true, LocalTime.of(8, 0), LocalTime.of(13, 0), 60));
            } else {
                disponibilidadeRepository.save(new DisponibilidadeSemanal(dia, true, LocalTime.of(8, 0), LocalTime.of(18, 0), 60));
            }
        }

        LocalDate hoje = LocalDate.now(clock);
        LocalDate proximoDiaUtil = proximoDiaAtivo(hoje.plusDays(1));
        LocalDate segundoDiaUtil = proximoDiaAtivo(proximoDiaUtil.plusDays(1));
        LocalDate diaFolgaFutura = proximoDiaAtivo(segundoDiaUtil.plusDays(4));

        bloqueioRepository.save(new BloqueioAgenda(diaFolgaFutura, "Curso NR-10 / Reciclagem Técnica"));

        // 2. Clientes fictícios representativos das Personas (Residencial e Empresarial/Condomínio)
        Cliente c1 = new Cliente("Mariana Ferreira (Fictício)", "16991234567",
                "Rua São Sebastião, 410 - Centro, Ribeirão Preto", PerfilCliente.RESIDENCIAL);
        c1.setEmail("mariana.demo@exemplo.com");
        c1.setObservacoes("Apartamento 42 - Interfone na portaria");
        clienteRepository.save(c1);

        Cliente c2 = new Cliente("Condomínio Residencial Ipê Branco", "1636258899",
                "Av. João Fiúsa, 1420 - Jardim Irajá, Ribeirão Preto", PerfilCliente.EMPRESARIAL_CONDOMINIO);
        c2.setEmail("sindico.ipe@exemplo.com");
        c2.setObservacoes("Procurar zelador Marcos; manutenção preventiva mensal");
        clienteRepository.save(c2);

        Cliente c3 = new Cliente("Padaria Artesanal Pão Quente Ltda", "16992345678",
                "Av. Independência, 890 - Sumaré, Ribeirão Preto", PerfilCliente.EMPRESARIAL_CONDOMINIO);
        c3.setEmail("contato@paoquente-demo.com");
        c3.setObservacoes("Quadro trifásico dos fornos industriais");
        clienteRepository.save(c3);

        Cliente c4 = new Cliente("Lucas Andrade (Fictício)", "16997776655",
                "Rua Conde Afonso Celso, 620 - Boulevard, Ribeirão Preto", PerfilCliente.RESIDENCIAL);
        clienteRepository.save(c4);

        LocalDateTime agora = LocalDateTime.now(clock);

        // 3. Agendamentos de demonstração (passado concluído, hoje e próximos dias)
        LocalDate diaHojeAgenda = hoje.getDayOfWeek() == DayOfWeek.SUNDAY ? proximoDiaUtil : hoje;

        Agendamento a1 = criarSeed("PP-2026-0101", c1, diaHojeAgenda, LocalTime.of(9, 0),
                TipoServico.INSTALACAO_ELETRICA, c1.getEnderecoPadrao(),
                "Substituição de disjuntor e instalação de chuveiro 220V", StatusAgendamento.CONCLUIDO, agora.minusHours(3));
        a1.setObservacaoTecnica("Instalado disjuntor bipolar 40A + cabo 6mm².");
        agendamentoRepository.save(a1);
        historicoRepository.save(new HistoricoStatusAgendamento(a1, StatusAgendamento.CONFIRMADO, StatusAgendamento.CONCLUIDO,
                "Leonardo (Proprietário Power Plus)", "Serviço concluído e testado", agora.minusHours(1)));

        Agendamento a2 = criarSeed("PP-2026-0102", c2, diaHojeAgenda, LocalTime.of(14, 0),
                TipoServico.MANUTENCAO_PREVENTIVA, c2.getEnderecoPadrao(),
                "Inspeção preventiva nas bombas do reservatório e iluminação de emergência", StatusAgendamento.CONFIRMADO, agora.minusHours(5));

        Agendamento a3 = criarSeed("PP-2026-0103", c3, proximoDiaUtil, LocalTime.of(10, 0),
                TipoServico.QUADRO_DISJUNTORES, c3.getEnderecoPadrao(),
                "Balanceamento de fases e reaperto de barramentos do quadro principal", StatusAgendamento.CONFIRMADO, agora.minusHours(2));

        Agendamento a4 = criarSeed("PP-2026-0104", c4, segundoDiaUtil, LocalTime.of(15, 0),
                TipoServico.VISTORIA_ORCAMENTO, c4.getEnderecoPadrao(),
                "Orçamento para pontos de ar-condicionado nos quartos", StatusAgendamento.AGUARDANDO_APROVACAO, agora.minusMinutes(40));

        Agendamento a5 = criarSeed("PP-2026-0105", c1, proximoDiaUtil, LocalTime.of(16, 0),
                TipoServico.REPARO_ELETRICO, c1.getEnderecoPadrao(),
                "Tomada da lavanderia aquecendo ao ligar secadora", StatusAgendamento.AGUARDANDO_APROVACAO, agora.minusMinutes(15));

        // 4. Conversa no Chat aguardando transbordo humano para demonstração ao vivo
        ConversaChat chat = new ConversaChat("Roberto (Síndico Cond. Ipê)", "1636258899", agora.minusMinutes(25));
        chat.adicionarMensagem("ASSISTENTE",
                "Olá, Roberto! Sou o assistente virtual da Power Plus. Como posso ajudar?", agora.minusMinutes(25));
        chat.adicionarMensagem("CLIENTE",
                "Preciso verificar se conseguimos incluir termografia do QGBT na visita preventiva de hoje às 14h.", agora.minusMinutes(24));
        chat.adicionarMensagem("ASSISTENTE",
                "Sua solicitação envolve detalhes específicos que precisam da avaliação direta do eletricista. Encaminhei nossa conversa com todo o histórico para o proprietário assumir o atendimento!",
                agora.minusMinutes(24));
        chat.setStatus(StatusConversaChat.AGUARDANDO_HUMANO);
        conversaChatRepository.save(chat);

        // 5. Notificações recentes
        notificacaoRepository.save(new NotificacaoRegistro(
                TipoNotificacao.SOLICITACAO_RECEBIDA, "PROPRIETARIO",
                "Aprovação pendente: " + c1.getNome(),
                "Protocolo " + a5.getProtocolo() + " aguarda sua aprovação para às 16:00 – Reparo Elétrico.",
                a5.getProtocolo(), "SISTEMA_INTERNO", agora.minusMinutes(15)
        ));
        notificacaoRepository.save(new NotificacaoRegistro(
                TipoNotificacao.CONFIRMACAO, "PROPRIETARIO",
                "Novo agendamento confirmado: " + c2.getNome(),
                "Protocolo " + a2.getProtocolo() + " agendado às 14:00 – Manutenção Preventiva.",
                a2.getProtocolo(), "SISTEMA_INTERNO", agora.minusHours(2)
        ));
        notificacaoRepository.save(new NotificacaoRegistro(
                TipoNotificacao.TRANSBORDO_CHAT, "PROPRIETARIO",
                "Cliente aguarda atendimento humano no chat",
                "Roberto (Síndico Cond. Ipê) solicitou avaliação sobre termografia do QGBT.",
                "CHAT-1", "SISTEMA_INTERNO", agora.minusMinutes(24)
        ));
    }

    private Agendamento criarSeed(String protocolo, Cliente cliente, LocalDate data, LocalTime inicio,
                                  TipoServico servico, String endereco, String descricao,
                                  StatusAgendamento status, LocalDateTime criadoEm) {
        Agendamento a = new Agendamento(protocolo, cliente, data, inicio, inicio.plusMinutes(60),
                servico, endereco, descricao);
        a.setStatus(status);
        a.setCriadoEm(criadoEm);
        a.setAtualizadoEm(criadoEm);
        Agendamento salvo = agendamentoRepository.save(a);
        historicoRepository.save(new HistoricoStatusAgendamento(
                salvo, null, status == StatusAgendamento.AGUARDANDO_APROVACAO
                        ? StatusAgendamento.AGUARDANDO_APROVACAO
                        : StatusAgendamento.CONFIRMADO,
                "CLIENTE (Autoatendimento)",
                status == StatusAgendamento.AGUARDANDO_APROVACAO
                        ? "Solicitação enviada — aguardando aprovação do proprietário"
                        : "Agendamento registrado e aprovado",
                criadoEm
        ));
        return salvo;
    }

    private LocalDate proximoDiaAtivo(LocalDate base) {
        LocalDate d = base;
        while (d.getDayOfWeek() == DayOfWeek.SUNDAY) {
            d = d.plusDays(1);
        }
        return d;
    }
}

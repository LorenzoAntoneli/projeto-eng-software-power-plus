package br.edu.fatec.rp.powerplus.config;

import br.edu.fatec.rp.powerplus.domain.*;
import br.edu.fatec.rp.powerplus.repository.*;
import br.edu.fatec.rp.powerplus.service.AutenticacaoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Cria o usuário padrão do eletricista e preenche dados iniciais de demonstração
 * caso o banco de dados H2 ainda esteja vazio.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final boolean seedEnabled;
    private final AutenticacaoService authService;
    private final DisponibilidadeSemanalRepository disponibilidadeRepository;
    private final BloqueioAgendaRepository bloqueioRepository;
    private final ClienteRepository clienteRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final HistoricoStatusRepository historicoRepository;
    private final ConversaChatRepository conversaChatRepository;
    private final NotificacaoRegistroRepository notificacaoRepository;

    public DataSeeder(@Value("${powerplus.seed.enabled:true}") boolean seedEnabled,
                      AutenticacaoService authService,
                      DisponibilidadeSemanalRepository disponibilidadeRepository,
                      BloqueioAgendaRepository bloqueioRepository,
                      ClienteRepository clienteRepository,
                      AgendamentoRepository agendamentoRepository,
                      HistoricoStatusRepository historicoRepository,
                      ConversaChatRepository conversaChatRepository,
                      NotificacaoRegistroRepository notificacaoRepository) {
        this.seedEnabled = seedEnabled;
        this.authService = authService;
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.clienteRepository = clienteRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.historicoRepository = historicoRepository;
        this.conversaChatRepository = conversaChatRepository;
        this.notificacaoRepository = notificacaoRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        authService.garantirUsuarioPadrao();
        if (!seedEnabled || disponibilidadeRepository.count() > 0) {
            return;
        }

        // 1. Expediente padrão da semana (Seg–Sex 08h–18h, Sáb 08h–13h, Dom fechado)
        for (DayOfWeek dia : DayOfWeek.values()) {
            boolean ativo = dia != DayOfWeek.SUNDAY;
            LocalTime fim = (dia == DayOfWeek.SUNDAY) ? LocalTime.of(12, 0)
                    : (dia == DayOfWeek.SATURDAY) ? LocalTime.of(13, 0) : LocalTime.of(18, 0);
            disponibilidadeRepository.save(new DisponibilidadeSemanal(dia, ativo, LocalTime.of(8, 0), fim, 60));
        }

        LocalDate hoje = LocalDate.now();
        LocalDate proximoDiaUtil = pularDomingo(hoje.plusDays(1));
        LocalDate segundoDiaUtil = pularDomingo(proximoDiaUtil.plusDays(1));
        bloqueioRepository.save(new BloqueioAgenda(pularDomingo(segundoDiaUtil.plusDays(4)), "Curso externo / reciclagem técnica"));

        // 2. Clientes de exemplo
        Cliente c1 = salvarCliente("Mariana Ferreira", "16991234567", "mariana.ferreira@exemplo.com",
                "Rua São Sebastião, 410 - Centro, Ribeirão Preto", PerfilCliente.RESIDENCIAL, "Apartamento 42 - Interfone na portaria");
        Cliente c2 = salvarCliente("Condomínio Residencial Ipê Branco", "1636258899", "sindico.ipe@exemplo.com",
                "Av. João Fiúsa, 1420 - Jardim Irajá, Ribeirão Preto", PerfilCliente.EMPRESARIAL_CONDOMINIO, "Procurar zelador Marcos; manutenção preventiva mensal");
        Cliente c3 = salvarCliente("Padaria Artesanal Pão Quente Ltda", "16992345678", "contato@paoquente.com",
                "Av. Independência, 890 - Sumaré, Ribeirão Preto", PerfilCliente.EMPRESARIAL_CONDOMINIO, "Quadro trifásico dos fornos industriais");
        Cliente c4 = salvarCliente("Lucas Andrade", "16997776655", null,
                "Rua Conde Afonso Celso, 620 - Boulevard, Ribeirão Preto", PerfilCliente.RESIDENCIAL, null);

        // 3. Agendamentos iniciais
        LocalDateTime agora = LocalDateTime.now();
        LocalDate diaHojeAgenda = pularDomingo(hoje);

        Agendamento a1 = salvarSeed("PP-2026-0101", c1, diaHojeAgenda, LocalTime.of(9, 0), TipoServico.INSTALACAO_ELETRICA,
                "Substituição de disjuntor e instalação de chuveiro 220V", StatusAgendamento.CONCLUIDO, agora.minusHours(3));
        a1.setObservacaoTecnica("Instalado disjuntor bipolar 40A + cabo 6mm².");
        agendamentoRepository.save(a1);
        historicoRepository.save(new HistoricoStatusAgendamento(a1, StatusAgendamento.CONFIRMADO, StatusAgendamento.CONCLUIDO,
                "Leonardo (Proprietário Power Plus)", "Serviço concluído e testado", agora.minusHours(1)));

        Agendamento a2 = salvarSeed("PP-2026-0102", c2, diaHojeAgenda, LocalTime.of(14, 0), TipoServico.MANUTENCAO_PREVENTIVA,
                "Inspeção preventiva nas bombas do reservatório e iluminação de emergência", StatusAgendamento.CONFIRMADO, agora.minusHours(5));
        salvarSeed("PP-2026-0103", c3, proximoDiaUtil, LocalTime.of(10, 0), TipoServico.QUADRO_DISJUNTORES,
                "Balanceamento de fases e reaperto de barramentos do quadro principal", StatusAgendamento.CONFIRMADO, agora.minusHours(2));
        salvarSeed("PP-2026-0104", c4, segundoDiaUtil, LocalTime.of(15, 0), TipoServico.VISTORIA_ORCAMENTO,
                "Orçamento para pontos de ar-condicionado nos quartos", StatusAgendamento.AGUARDANDO_APROVACAO, agora.minusMinutes(40));
        Agendamento a5 = salvarSeed("PP-2026-0105", c1, proximoDiaUtil, LocalTime.of(16, 0), TipoServico.REPARO_ELETRICO,
                "Tomada da lavanderia aquecendo ao ligar secadora", StatusAgendamento.AGUARDANDO_APROVACAO, agora.minusMinutes(15));

        // 4. Conversa e notificações iniciais
        ConversaChat chat = new ConversaChat("Roberto (Síndico Cond. Ipê)", "1636258899", agora.minusMinutes(25));
        chat.adicionarMensagem("ASSISTENTE", "Olá, Roberto! Sou o assistente virtual da Power Plus. Como posso ajudar?", agora.minusMinutes(25));
        chat.adicionarMensagem("CLIENTE", "Preciso verificar se conseguimos incluir termografia do QGBT na visita preventiva de hoje às 14h.", agora.minusMinutes(24));
        chat.adicionarMensagem("ASSISTENTE", "Sua solicitação envolve detalhes específicos que precisam da avaliação direta do eletricista. Encaminhei nossa conversa com todo o histórico para o proprietário assumir o atendimento!", agora.minusMinutes(24));
        chat.setStatus(StatusConversaChat.AGUARDANDO_HUMANO);
        conversaChatRepository.save(chat);

        notificacaoRepository.save(new NotificacaoRegistro(TipoNotificacao.SOLICITACAO_RECEBIDA, "PROPRIETARIO",
                "Aprovação pendente: " + c1.getNome(), "Protocolo " + a5.getProtocolo() + " aguarda sua aprovação para às 16:00 – Reparo Elétrico.",
                a5.getProtocolo(), "SISTEMA_INTERNO", agora.minusMinutes(15)));
        notificacaoRepository.save(new NotificacaoRegistro(TipoNotificacao.CONFIRMACAO, "PROPRIETARIO",
                "Novo agendamento confirmado: " + c2.getNome(), "Protocolo " + a2.getProtocolo() + " agendado às 14:00 – Manutenção Preventiva.",
                a2.getProtocolo(), "SISTEMA_INTERNO", agora.minusHours(2)));
        notificacaoRepository.save(new NotificacaoRegistro(TipoNotificacao.TRANSBORDO_CHAT, "PROPRIETARIO",
                "Cliente aguarda atendimento humano no chat", "Roberto (Síndico Cond. Ipê) solicitou avaliação sobre termografia do QGBT.",
                "CHAT-1", "SISTEMA_INTERNO", agora.minusMinutes(24)));
    }

    private Cliente salvarCliente(String nome, String tel, String email, String endereco, PerfilCliente perfil, String obs) {
        Cliente c = new Cliente(nome, tel, endereco, perfil);
        c.setEmail(email);
        c.setObservacoes(obs);
        return clienteRepository.save(c);
    }

    private Agendamento salvarSeed(String protocolo, Cliente cliente, LocalDate data, LocalTime inicio,
                                   TipoServico servico, String descricao, StatusAgendamento status, LocalDateTime criadoEm) {
        Agendamento a = new Agendamento(protocolo, cliente, data, inicio, inicio.plusMinutes(60), servico, cliente.getEnderecoPadrao(), descricao);
        a.setStatus(status);
        a.setCriadoEm(criadoEm);
        a.setAtualizadoEm(criadoEm);
        Agendamento salvo = agendamentoRepository.save(a);
        boolean pendente = status == StatusAgendamento.AGUARDANDO_APROVACAO;
        historicoRepository.save(new HistoricoStatusAgendamento(salvo, null,
                pendente ? StatusAgendamento.AGUARDANDO_APROVACAO : StatusAgendamento.CONFIRMADO,
                "CLIENTE (Autoatendimento)",
                pendente ? "Solicitação enviada — aguardando aprovação do proprietário" : "Agendamento registrado e aprovado",
                criadoEm));
        return salvo;
    }

    private LocalDate pularDomingo(LocalDate data) {
        return data.getDayOfWeek() == DayOfWeek.SUNDAY ? data.plusDays(1) : data;
    }
}

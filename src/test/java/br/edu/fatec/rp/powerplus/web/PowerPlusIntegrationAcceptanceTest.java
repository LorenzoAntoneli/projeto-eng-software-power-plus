package br.edu.fatec.rp.powerplus.web;

import br.edu.fatec.rp.powerplus.domain.DisponibilidadeSemanal;
import br.edu.fatec.rp.powerplus.repository.DisponibilidadeSemanalRepository;
import br.edu.fatec.rp.powerplus.service.AutenticacaoProprietarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PowerPlusIntegrationAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DisponibilidadeSemanalRepository disponibilidadeRepository;

    @Autowired
    private AutenticacaoProprietarioService authService;

    @Autowired
    private Clock clock;

    private LocalDate proximaDataUtil;

    @BeforeEach
    void configurarExpedientePadrao() {
        disponibilidadeRepository.deleteAll();
        for (DayOfWeek dia : DayOfWeek.values()) {
            boolean ativo = dia != DayOfWeek.SUNDAY;
            disponibilidadeRepository.save(new DisponibilidadeSemanal(
                    dia, ativo, LocalTime.of(8, 0), LocalTime.of(18, 0), 60
            ));
        }
        LocalDate d = LocalDate.now(clock).plusDays(2);
        if (d.getDayOfWeek() == DayOfWeek.SUNDAY) {
            d = d.plusDays(1);
        }
        proximaDataUtil = d;
    }

    private String autenticarProprietario() throws Exception {
        String loginJson = """
                {"username":"leonardo.powerplus","password":"PowerPlus@2026"}
                """;
        MvcResult res = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("CT01 / TI01 - Aceitação: Agendar visita válida, bloquear vaga na lista livre e exibir na agenda (HU01/HU02/HU06/HU08)")
    void deveAgendarVisitaBloquearHorarioEExibirNaAgenda() throws Exception {
        mockMvc.perform(get("/api/publico/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiposServico", not(empty())));

        String payload = String.format("""
                {
                  "nomeCliente": "Mariana Ferreira",
                  "telefone": "(16) 99123-4567",
                  "email": "mariana@exemplo.com",
                  "endereco": "Rua São Sebastião, 410 - Centro, Ribeirão Preto",
                  "perfilCliente": "RESIDENCIAL",
                  "data": "%s",
                  "horaInicio": "14:00",
                  "tipoServico": "INSTALACAO_ELETRICA",
                  "descricaoServico": "Instalação de chuveiro 220V"
                }
                """, proximaDataUtil);

        MvcResult createRes = mockMvc.perform(post("/api/publico/agendamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protocolo", startsWith("PP-")))
                .andExpect(jsonPath("$.statusCodigo").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$.statusDescricao").value("Aguardando aprovação"))
                .andReturn();

        String protocolo = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("protocolo").asText();

        // Verifica que 14:00 deixou de aparecer nos horários livres (HU08)
        mockMvc.perform(get("/api/publico/horarios-livres").param("data", proximaDataUtil.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horariosLivres", not(hasItem("14:00"))))
                .andExpect(jsonPath("$.horariosLivres", hasItem("15:00")));

        // Consulta pelo protocolo e pelo telefone (HU12)
        mockMvc.perform(get("/api/publico/agendamentos/protocolo/" + protocolo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agendamento.protocolo").value(protocolo))
                .andExpect(jsonPath("$.historico", hasSize(1)));

        mockMvc.perform(get("/api/publico/agendamentos/cliente").param("telefone", "(16) 99123-4567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("CT02 / TI02 - Negativo: Impedir segundo agendamento no mesmo horário com HTTP 409 (RN01/HU08)")
    void deveRetornar409AoTentarAgendarHorarioJaOcupado() throws Exception {
        String primeiro = String.format("""
                {
                  "nomeCliente": "Cliente Um",
                  "telefone": "(16) 99111-1111",
                  "endereco": "Rua A, 100",
                  "perfilCliente": "RESIDENCIAL",
                  "data": "%s",
                  "horaInicio": "10:00",
                  "tipoServico": "REPARO_ELETRICO"
                }
                """, proximaDataUtil);

        String segundoConflitante = String.format("""
                {
                  "nomeCliente": "Cliente Dois",
                  "telefone": "(16) 99222-2222",
                  "endereco": "Rua B, 200",
                  "perfilCliente": "RESIDENCIAL",
                  "data": "%s",
                  "horaInicio": "10:00",
                  "tipoServico": "VISTORIA_ORCAMENTO"
                }
                """, proximaDataUtil);

        mockMvc.perform(post("/api/publico/agendamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(primeiro))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/publico/agendamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(segundoConflitante))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_NEGOCIO"));
    }

    @Test
    @DisplayName("CT03 / TI03 - Negativo: Retornar HTTP 400 quando campos obrigatórios estiverem ausentes (HU02)")
    void deveRetornar400QuandoCamposObrigatoriosVazios() throws Exception {
        String invalido = """
                {
                  "nomeCliente": "",
                  "telefone": "",
                  "endereco": ""
                }
                """;
        mockMvc.perform(post("/api/publico/agendamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_CAMPOS"))
                .andExpect(jsonPath("$.campos.nomeCliente").exists());
    }

    @Test
    @DisplayName("CT04 / TI04 - Aceitação: Remarcar e cancelar agendamento liberando o horário anterior (HU04/UC02)")
    void deveRemarcarECancelarLiberandoVagaNaAgenda() throws Exception {
        String payload = String.format("""
                {
                  "nomeCliente": "Condomínio Bela Vista",
                  "telefone": "(16) 3625-9900",
                  "endereco": "Av. João Fiúsa, 900",
                  "perfilCliente": "EMPRESARIAL_CONDOMINIO",
                  "data": "%s",
                  "horaInicio": "09:00",
                  "tipoServico": "MANUTENCAO_PREVENTIVA"
                }
                """, proximaDataUtil);

        MvcResult res = mockMvc.perform(post("/api/publico/agendamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        String protocolo = objectMapper.readTree(res.getResponse().getContentAsString()).get("protocolo").asText();

        // Remarca de 09:00 para 16:00
        String remarcacao = String.format("""
                {"novaData": "%s", "novaHoraInicio": "16:00", "motivo": "Ajuste com zelador"}
                """, proximaDataUtil);
        mockMvc.perform(put("/api/publico/agendamentos/" + protocolo + "/remarcar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(remarcacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horaInicio").value("16:00"));

        // 09:00 volta a ficar livre!
        mockMvc.perform(get("/api/publico/horarios-livres").param("data", proximaDataUtil.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horariosLivres", hasItem("09:00")))
                .andExpect(jsonPath("$.horariosLivres", not(hasItem("16:00"))));

        // Cancela o agendamento das 16:00 -> 16:00 também volta a ficar livre!
        mockMvc.perform(post("/api/publico/agendamentos/" + protocolo + "/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Reunião extraordinária\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCodigo").value("CANCELADO"));

        mockMvc.perform(get("/api/publico/horarios-livres").param("data", proximaDataUtil.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horariosLivres", hasItem("16:00")));
    }

    @Test
    @DisplayName("CT05 / TI05 - Aceitação: Fluxo integrado de Chat FAQ, Transbordo Humano e Resposta do Proprietário (HU05/HU11/UC03)")
    void deveExecutarFluxoCompletoDeChatETransbordoHumano() throws Exception {
        // 1. Cliente tira dúvida comum sobre orçamento
        MvcResult faqRes = mockMvc.perform(post("/api/publico/chat/mensagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCliente":"Carlos Cliente","telefoneCliente":"16998887777","mensagem":"Qual o valor da visita e orçamento?","solicitarHumano":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCodigo").value("AUTOMATICO"))
                .andReturn();

        long conversaId = objectMapper.readTree(faqRes.getResponse().getContentAsString()).get("id").asLong();

        // 2. Cliente solicita atendimento humano -> transbordo AGUARDANDO_HUMANO
        mockMvc.perform(post("/api/publico/chat/mensagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"conversaId":%d,"mensagem":"Quero falar com o proprietário sobre entrada trifásica","solicitarHumano":true}
                                """, conversaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCodigo").value("AGUARDANDO_HUMANO"));

        // 3. Proprietário autentica, consulta conversas e responde
        String token = autenticarProprietario();
        mockMvc.perform(post("/api/admin/conversas/" + conversaId + "/responder")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resposta":"Olá! Posso avaliar o padrão trifásico amanhã às 10h.","encerrar":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCodigo").value("EM_ATENDIMENTO_HUMANO"));

        mockMvc.perform(get("/api/publico/chat/" + conversaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagens[?(@.remetente == 'PROPRIETARIO')]").isNotEmpty());
    }

    @Test
    @DisplayName("CT06 / TI06 - Segurança & Operação Admin: Bloqueio 401 sem token, Dashboard, Status, Clientes e Bloqueios (RNF03/RN05/RN06)")
    void deveProtegerRotasAdminEOperarFluxoCompletoDoProprietario() throws Exception {
        // Sem token -> 401 Unauthorized (RNF03)
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());

        // Senha incorreta -> 401 Unauthorized
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"carlos.powerplus\",\"password\":\"senhaErrada\"}"))
                .andExpect(status().isUnauthorized());

        String token = autenticarProprietario();

        // Cadastra cliente manualmente e valida bloqueio de duplicidade (RN05)
        String cliJson = """
                {
                  "nome": "Clínica Odontológica Sorriso",
                  "telefone": "(16) 3610-2030",
                  "email": "contato@clinica.com",
                  "endereco": "Rua Bernardino de Campos, 1000",
                  "perfil": "EMPRESARIAL_CONDOMINIO",
                  "observacoes": "Atendimento após 18h ou sábado"
                }
                """;
        MvcResult cliRes = mockMvc.perform(post("/api/admin/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cliJson))
                .andExpect(status().isCreated())
                .andReturn();
        long clienteId = objectMapper.readTree(cliRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/admin/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cliJson))
                .andExpect(status().isConflict());

        // Cria agendamento, consulta dashboard/agenda semanal, envia lembrete e conclui atendimento
        String agJson = String.format("""
                {
                  "nomeCliente": "Clínica Odontológica Sorriso",
                  "telefone": "(16) 3610-2030",
                  "endereco": "Rua Bernardino de Campos, 1000",
                  "perfilCliente": "EMPRESARIAL_CONDOMINIO",
                  "data": "%s",
                  "horaInicio": "11:00",
                  "tipoServico": "QUADRO_DISJUNTORES"
                }
                """, proximaDataUtil);

        MvcResult agRes = mockMvc.perform(post("/api/publico/agendamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agJson))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode agNode = objectMapper.readTree(agRes.getResponse().getContentAsString());
        long agendamentoId = agNode.get("id").asLong();

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token)
                        .param("data", proximaDataUtil.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHoje").value(1))
                .andExpect(jsonPath("$.pendentesAprovacaoTotal").value(1))
                .andExpect(jsonPath("$.whatsappFeatureFlagAtiva").value(false));

        mockMvc.perform(get("/api/admin/agenda/semana")
                        .header("Authorization", "Bearer " + token)
                        .param("dataReferencia", proximaDataUtil.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agendamentos", hasSize(1)));

        // Proprietário Leonardo aprova a solicitação antes de enviar lembrete e iniciar atendimento
        mockMvc.perform(post("/api/admin/agendamentos/" + agendamentoId + "/aprovar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"Visita aprovada pelo Leonardo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCodigo").value("CONFIRMADO"));

        mockMvc.perform(post("/api/admin/agendamentos/" + agendamentoId + "/lembrete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Tenta bloquear a data com agendamento ativo sem forçar -> 422 Regra de Negócio (RN06)
        mockMvc.perform(post("/api/admin/bloqueios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"data\":\"%s\",\"motivo\":\"Folga\",\"forcarComConflito\":false}", proximaDataUtil)))
                .andExpect(status().isUnprocessableEntity());

        // Avança status para EM_ATENDIMENTO e depois CONCLUIDO e confere histórico de auditoria (RNF06)
        mockMvc.perform(patch("/api/admin/agendamentos/" + agendamentoId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"EM_ATENDIMENTO\",\"observacao\":\"Iniciado no local\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCodigo").value("EM_ATENDIMENTO"));

        mockMvc.perform(patch("/api/admin/agendamentos/" + agendamentoId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"CONCLUIDO\",\"observacao\":\"Quadro revisado com sucesso\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCodigo").value("CONCLUIDO"));

        mockMvc.perform(get("/api/admin/agendamentos/" + agendamentoId + "/historico")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));

        mockMvc.perform(get("/api/admin/clientes/" + clienteId + "/historico")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/admin/notificacoes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));

        mockMvc.perform(post("/api/admin/notificacoes/marcar-lidas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CT07 - Navegação multi-telas: Deve resolver rotas separadas do Portal do Cliente e do Painel Operacional")
    void deveResolverTodasAsRotasSeparadasDeTelasWeb() throws Exception {
        for (String rota : new String[]{
                "/agendar", "/comprovante", "/acompanhar", "/atendimento",
                "/painel/login", "/painel/agenda", "/painel/expediente",
                "/painel/clientes", "/painel/mensagens", "/painel/avisos"
        }) {
            mockMvc.perform(get(rota))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/painel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/painel/agenda"));
    }

    @Test
    @DisplayName("CT08 / TI08 - Aceitação: Fluxo completo de Aprovar e Recusar solicitações pendentes pelo Leonardo")
    void deveAprovarERecusarSolicitacoesLiberandoHorarioQuandoRecusado() throws Exception {
        String pedidoAprovar = String.format("""
                {
                  "nomeCliente": "Fernanda Souza",
                  "telefone": "(16) 99444-5566",
                  "endereco": "Rua Duque de Caxias, 320",
                  "perfilCliente": "RESIDENCIAL",
                  "data": "%s",
                  "horaInicio": "13:00",
                  "tipoServico": "INSTALACAO_ELETRICA"
                }
                """, proximaDataUtil);

        String pedidoRecusar = String.format("""
                {
                  "nomeCliente": "Oficina Mecânica Rápida",
                  "telefone": "(16) 3633-7788",
                  "endereco": "Av. Saudade, 750",
                  "perfilCliente": "EMPRESARIAL_CONDOMINIO",
                  "data": "%s",
                  "horaInicio": "15:00",
                  "tipoServico": "REPARO_ELETRICO"
                }
                """, proximaDataUtil);

        MvcResult r1 = mockMvc.perform(post("/api/publico/agendamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoAprovar))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCodigo").value("AGUARDANDO_APROVACAO"))
                .andReturn();
        long idAprovar = objectMapper.readTree(r1.getResponse().getContentAsString()).get("id").asLong();

        MvcResult r2 = mockMvc.perform(post("/api/publico/agendamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoRecusar))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCodigo").value("AGUARDANDO_APROVACAO"))
                .andReturn();
        long idRecusar = objectMapper.readTree(r2.getResponse().getContentAsString()).get("id").asLong();

        String token = autenticarProprietario();

        // Aprova o das 13:00 -> permanece ocupado como CONFIRMADO
        mockMvc.perform(post("/api/admin/agendamentos/" + idAprovar + "/aprovar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"Confirmado na rota da tarde\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCodigo").value("CONFIRMADO"));

        // Recusa o das 15:00 -> muda para RECUSADO e libera imediatamente o horário 15:00!
        mockMvc.perform(post("/api/admin/agendamentos/" + idRecusar + "/recusar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"Manutenção emergencial externa no horário\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCodigo").value("RECUSADO"))
                .andExpect(jsonPath("$.statusDescricao").value("Recusado"));

        mockMvc.perform(get("/api/publico/horarios-livres").param("data", proximaDataUtil.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horariosLivres", not(hasItem("13:00"))))
                .andExpect(jsonPath("$.horariosLivres", hasItem("15:00")));
    }
}

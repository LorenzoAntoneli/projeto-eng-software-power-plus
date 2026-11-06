# ⚡ Power Plus – Sistema de Agendamento de Clientes e Atendimento Automatizado

> **Projeto Integrador – Fábrica de Software (2º Semestre de 2026)**  
> **Instituição:** FATEC Ribeirão Preto – Curso Superior de Tecnologia em Análise e Desenvolvimento de Sistemas  
> **Disciplina:** Engenharia de Software II  
> **Docente responsável:** Profa. Me. Eliza Bomfim  
> **Repositório Oficial no GitHub:** https://github.com/LorenzoAntoneli/projeto-eng-software-power-plus

---

## 1. Sobre o Projeto e o Problema Real

A **Power Plus** é uma empresa de serviços elétricos residenciais, comerciais e condominiais em Ribeirão Preto – SP, cujo proprietário atua de forma autônoma executando as instalações/manutenções em campo e gerenciando todo o atendimento aos clientes.

Antes desta solução, o agendamento ocorria manualmente por WhatsApp e ligações telefônicas, ocasionando:
- Horas diárias perdidas em conversas repetitivas para combinar horários;
- Interrupções constantes durante serviços elétricos em andamento;
- Risco de choque de horários ou esquecimento de compromissos;
- Dispersão de endereços, especificações técnicas e históricos de atendimento;
- Perda de potenciais clientes pela demora no retorno.

### Solução Entregue (MVP `v1.0.0`)
Aplicação web responsiva (acessível via smartphone e computador) estruturada em **6 funcionalidades principais** conforme a Regra 11.1 do Edital:
1. **Gestão de Clientes e Histórico (`RF01`, `RF07`, `HU09`, `HU12`, `UC06`)**: Cadastro automático e manual com normalização de telefone, alerta de duplicidade (`RN05`) e consulta de histórico por cliente residencial ou empresarial/condomínio.
2. **Configuração de Disponibilidade e Bloqueios (`RF02`, `HU07`, `UC04`)**: Definição de dias/janelas de atendimento semanal e bloqueio de folgas/feriados com alerta de conflitos existentes (`RN06`).
3. **Autoatendimento de Agendamento Sem Conflitos (`RF03`, `RF04`, `HU01`, `HU02`, `HU08`, `UC01`)**: Exibição exclusiva de slots livres reais, geração de protocolo único (`PP-2026-XXXX`) e bloqueio contra dupla reserva (`RN01`).
4. **Remarcação, Cancelamento e Máquina de Status Auditada (`RF05`, `RF09`, `HU04`, `UC02`)**: Autoatendimento de remarcação e cancelamento com antecedência mínima de 2 horas (`RN03`), liberação automática de vaga e transições válidas auditadas (`RN04`, `RNF06`).
5. **Agenda Diária/Semanal e Dashboard Operacional (`RF06`, `RF08`, `HU06`, `HU10`, `UC05`, `UC07`)**: Painel autenticado (`BCrypt` – `RNF03`) com KPIs do dia, visão diária e semanal, central de notificações e disparo de lembretes.
6. **Chat de Atendimento Automatizado com Transbordo Humano (`RF10`, `HU05`, `HU11`, `UC03`)**: Assistente virtual integrado que responde dúvidas frequentes, consulta horários livres em tempo real e encaminha casos complexos ao proprietário com todo o histórico da conversa.

> **Feature Flag DevOps (`powerplus.features.whatsapp-enabled=false`)**: O módulo de notificações utiliza a interface `NotificacaoGateway`, mantendo o envio externo via API WhatsApp desativado por *feature flag* no escopo acadêmico do MVP e pronto para evolução futura.

---

## 2. Tecnologias Utilizadas

- **Linguagem:** Java 21 LTS (Eclipse Temurin)
- **Framework Backend:** Spring Boot 3.4.3 (Spring Web, Spring Data JPA, Bean Validation, Spring Security Crypto `BCrypt`)
- **Banco de Dados:** H2 Database (persistência local em arquivo `./data/powerplus-db` em execução normal e em memória na suíte de testes)
- **Frontend:** HTML5 Semântico, CSS3 Responsivo (Mobile-First) e JavaScript ES2022 (`Fetch API`)
- **Qualidade e Testes:** JUnit 5, Mockito, Spring MockMvc e JaCoCo `0.8.12` (Quality Gate automático no `mvn verify`)
- **DevOps & CI/CD:** Git (Gitflow simplificado `main`, `develop`, `feature/*`) + GitHub Actions (`.github/ci-workflow.yml`) + Maven Wrapper (`mvnw` / `mvnw.cmd`)

---

## 3. Como Executar Localmente

### Pré-requisitos
- **JDK 21** configurado em `JAVA_HOME` (ou no `PATH`).
- Não é necessário instalar o Maven separadamente (o projeto inclui `./mvnw` e `mvnw.cmd`).

### Subir a Aplicação (MVP Funcional)
No terminal (PowerShell, CMD ou Bash), na raiz do projeto:

```powershell
.\mvnw.cmd spring-boot:run
```
*(No Linux/macOS: `./mvnw spring-boot:run`)*

Acesse no navegador:
- **Portal do Cliente (Telas Públicas Separadas):**
  - `http://localhost:8080/` — Agendamento de Visita em 2 etapas
  - `http://localhost:8080/acompanhar` — Consulta por protocolo/telefone, remarcação e cancelamento
  - `http://localhost:8080/comprovante?protocolo=PP-2026-0102` — Ordem / Comprovante imprimível
  - `http://localhost:8080/atendimento` — Central de Atendimento (triagem + transbordo em tempo real)
- **Sistema Operacional Interno (Telas do Proprietário):**
  - `http://localhost:8080/painel/login` — Autenticação (`carlos.powerplus` / `PowerPlus@2026`)
  - `http://localhost:8080/painel/agenda` — Agenda Diária e Semanal
  - `http://localhost:8080/painel/expediente` — Horários semanais e bloqueio de datas
  - `http://localhost:8080/painel/clientes` — Base de clientes e histórico de serviços
  - `http://localhost:8080/painel/mensagens` — Inbox de atendimento humano ao vivo
  - `http://localhost:8080/painel/avisos` — Registro de envios e avisos
- **Protocolos/Dados de Demonstração (Seed Fictício LGPD):**
  - Protocolo ativo para testar consulta/remarcação: `PP-2026-0102` ou `PP-2026-0103`
  - Telefone empresarial com histórico: `1636258899`

---

## 4. Como Executar os Testes Automatizados e Relatório de Cobertura

Para rodar toda a suíte (testes unitários, ciclo TDD, mocks/stubs Mockito, testes de integração/aceitação MockMvc e verificação de cobertura JaCoCo):

```powershell
.\mvnw.cmd clean verify
```

Após a execução, o relatório visual completo de cobertura JaCoCo estará disponível em:
- `target/site/jacoco/index.html`

---

## 5. Estrutura do Repositório

```text
├── .github/ci-workflow.yml           # Pipeline de Integração Contínua (GitHub Actions)
├── docs/
│   └── ROTEIRO_BANCA_DE_PRODUTO.md   # Guia objetivo para apresentação e demonstração na Banca
├── src/
│   ├── main/
│   │   ├── java/br/edu/fatec/rp/powerplus/
│   │   │   ├── config/               # ClockConfig (determinismo temporal) e DataSeeder (seed fictício)
│   │   │   ├── domain/               # Entidades JPA, Enums de estado e Exceções de negócio
│   │   │   ├── repository/           # Repositórios Spring Data JPA
│   │   │   ├── service/              # Regras de negócio, Validador TDD, Chat e Notificações
│   │   │   └── web/                  # Controllers REST, DTOs validados e GlobalExceptionHandler
│   │   └── resources/
│   │       ├── static/               # Interface Web Responsiva (index.html, css/, js/)
│   │       └── application.properties
│   └── test/                         # Suíte JUnit 5 + Mockito + Testes de Integração/Aceitação
├── Portfolio_Engenharia_Software_II_Power_Plus_ABNT.docx
└── pom.xml
```

---

## 6. Equipe de Desenvolvimento

- **Lorenzo Henrique Antoneli**
- **[INSERIR NOME COMPLETO DO INTEGRANTE 2]**
- **[INSERIR NOME COMPLETO DO INTEGRANTE 3]**
- **[INSERIR NOME COMPLETO DO INTEGRANTE 4]**

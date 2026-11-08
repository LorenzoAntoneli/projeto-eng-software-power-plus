# Roteiro Prático para a Banca de Produto – Power Plus (MVP v1.0.0)

Este documento organiza de forma direta a apresentação final da equipe conforme a **Seção 21 (Banca de Produto)** do Edital da disciplina de Engenharia de Software II (Profa. Me. Eliza Bomfim).

---

## Checklist Antes de Iniciar a Apresentação (2 minutos antes)

1. Iniciar o servidor Spring Boot na raiz do projeto:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
2. Abrir `http://localhost:8080` no navegador.
3. Deixar aberto em outra aba o relatório JaCoCo (`target/site/jacoco/index.html`) caso a banca solicite inspeção ao vivo da cobertura.

---

## Respostas Objetivas às 10 Questões Obrigatórias da Banca

### 1. O CLIENTE – Quem é o cliente?
- **Power Plus Serviços Elétricos** (Ribeirão Preto – SP), pequeno negócio em que o proprietário atua sozinho como eletricista responsável tanto pela execução técnica em campo (residências, comércios e condomínios) quanto por todo o atendimento comercial.

### 2. O PROBLEMA – Qual problema ou necessidade foi identificado?
- Agendamento 100% manual via WhatsApp e ligações durante o expediente técnico, gerando **perda de tempo em conversas repetitivas**, **interrupções durante serviços elétricos**, **risco de choque de horários**, **informações de endereço/serviço espalhadas** e **perda de clientes por demora na resposta**.

### 3. A DESCOBERTA – Como a equipe chegou à compreensão desse problema?
- Mapeamento do processo atual (*AS-IS*), entrevista semiestruturada ("Descubra antes de desenvolver") nas 4 dimensões do edital (Contexto, Processo Atual, Problemas e Necessidades) e construção de Personas e Mapas de Empatia para os 3 perfis reais: Proprietário, Cliente Residencial e Cliente Empresarial/Condomínio.

### 4. A SOLUÇÃO – Qual solução foi proposta?
- Uma aplicação web responsiva (mobile e desktop) que combina **autoatendimento de agendamento em horários realmente livres**, **chat automatizado com transbordo humano** e **painel operacional centralizado com agenda diária/semanal e histórico de clientes**.

### 5. O MVP – O que efetivamente foi desenvolvido?
Respeitando o limite de 5 a 7 funcionalidades do Edital (Seção 11.1), foram entregues **6 funcionalidades integradas**:
1. Gestão de clientes e histórico de serviços (`RF01`, `RF07`);
2. Configuração de disponibilidade semanal e bloqueio de folgas/feriados (`RF02`);
3. Agendamento online sem conflitos com protocolo imediato (`RF03`, `RF04`);
4. Remarcação/cancelamento com regra de antecedência mínima de 2h e máquina de status auditada (`RF05`, `RF09`);
5. Agenda diária/semanal, dashboard do dia e central de avisos/lembretes (`RF06`, `RF08`);
6. Chat automatizado para dúvidas frequentes com transbordo humano ao proprietário (`RF10`).
- **Fora do MVP (justificado):** integração externa paga com WhatsApp Cloud API (controlada por *Feature Flag*), emissão fiscal e cobrança financeira integrada.

### 6. Demonstração Ao Vivo do MVP (Centro da Banca – 4 a 5 minutos)
1. **Tela "Solicitar Visita" (`/` — Visão do Cliente):** usar um atalho rápido de data (`Amanhã`), escolher um horário livre, preencher os dados (com máscara automática de telefone) e clicar em **Solicitar agendamento**. Mostrar o comprovante gerado (`PP-2026-XXXX`) com o status **`Aguardando aprovação`** e explicar que o horário já ficou pré-reservado na agenda para evitar choque (`HU08`).
2. **Tela "Painel do Leonardo" (`/painel/login` → `/painel/agenda`):** autenticar (`leonardo.powerplus` / `PowerPlus@2026`), mostrar no topo da tela a fila destacada **"Solicitações aguardando sua aprovação"** e clicar em **✓ Aprovar visita** no pedido recém-criado (e demonstrar também o botão **✕ Recusar** em outro pedido pendente, liberando a vaga na hora). Voltar à aba do comprovante do cliente e atualizar a página para mostrar que o status mudou em tempo real para **`Confirmado`**!
3. **Tela "Atendimento Online" (`/atendimento` → `/painel/mensagens`):** clicar em *"Horários disponíveis"* (resposta automática consultando banco) e depois em *"Falar com Leonardo"* (demonstrando o transbordo `AGUARDANDO_HUMANO` com histórico preservado e resposta do proprietário).
4. **Tela "Meus Agendamentos" (`/acompanhar`):** consultar o histórico do condomínio pelo telefone `1636258899` ou remarcar/cancelar um protocolo respeitando a antecedência mínima de 2h.

### 7. A VALIDAÇÃO – Como o cliente participou e qual foi seu retorno?
- Validação em duas rodadas com o proprietário **Leonardo** (protótipos navegáveis e versão funcional do MVP). Principais mudanças implementadas a partir do retorno real do cliente:
  - **Exigência de aprovação manual de cada visita:** substituição da confirmação automática imediata pelo status **`Aguardando aprovação`**, com fila prioritária no topo da agenda e botões diretos **Aprovar visita** e **Recusar**;
  - **Interface mais fácil de usar no dia a dia:** separação em telas independentes sem poluição visual, atalhos de data em 1 clique (`Amanhã`, `Próximo dia útil`, `Próximo sábado`), máscara automática de telefone, setas rápidas `◀ / Hoje / ▶`, filtros por status e botão direto para chamar o cliente no WhatsApp.

### 8. A QUALIDADE – Quais evidências demonstram que a solução funciona?
- Suíte automatizada com **32 testes automatizados** (23 unitários com JUnit 5 + Mockito, incluindo os ciclos TDD de agendamento e aprovação, e 9 fluxos completos de integração/aceitação/segurança/negativos com Spring `MockMvc`), cobrindo **85,46% das linhas** e **85,23% das instruções** auditadas pelo plugin **JaCoCo**.

### 9. A ENGENHARIA E A EVOLUÇÃO – Quais técnicas foram aplicadas e o que foi refatorado?
- **Requisitos:** 3Cs, verificação INVEST, Casos de Uso (`UC01`–`UC07`) e Matriz de Rastreabilidade ponta a ponta.
- **TDD (`RED → GREEN → REFACTOR`):** commits rastreáveis na branch `feature/tdd-motor-agendamento` para prevenção de conflitos e antecedência mínima de 2h usando `Clock` injetável.
- **Refatoração de Code Smells:** extração de `PoliticaAgendamentoValidator` e `CalculadoraHorariosLivres` (*Extract Class* / SRP), substituição de strings mágicas por enums ricos (`StatusAgendamento.podeTransitarPara`), desacoplamento via DTOs `record` e `NotificacaoGateway` com *Feature Flag*.
- **Git & CI/CD:** fluxo `main`, `develop` e `feature/*` + pipeline GitHub Actions validando build, testes e cobertura a cada push/PR.

### 10. A PRÓXIMA VERSÃO – Próximos passos para um segundo semestre
1. Ativar a *Feature Flag* de integração oficial com WhatsApp Cloud API para envio externo de confirmações e lembretes automáticos;
2. Geração de ordem de serviço e orçamento em PDF com assinatura digital do cliente em campo;
3. Planos de manutenção preventiva recorrente mensal para condomínios e empresas;
4. Painel financeiro simplificado de orçamentos aprovados e recebimentos via PIX.

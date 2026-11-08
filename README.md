# Power Plus - Sistema de Agendamento

Projeto desenvolvido na disciplina de **Engenharia de Software II** da FATEC Ribeirão Preto (2º semestre de 2026), sob orientação da Profa. Eliza Bomfim.

Sistema web criado para a **Power Plus**, empresa de serviços elétricos em Ribeirão Preto, para organizar a marcação de visitas técnicas e o controle da agenda. Pelo site o cliente escolhe um horário livre e envia a solicitação, que passa pela aprovação do responsável no painel interno.

## Integrantes

- Lorenzo Henrique Antoneli
- Yuri de Souza Marcondes
- Cauã Margatho
- Diego da Silva Gallo

## Tecnologias

- Java 21 e Spring Boot
- Banco de dados H2
- HTML, CSS e JavaScript
- JUnit 5, Mockito e JaCoCo

## Como executar

Com o Java 21 instalado, execute na pasta raiz do projeto:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS:

```bash
./mvnw spring-boot:run
```

Após subir a aplicação, o portal do cliente fica disponível em `http://localhost:8080` e o painel interno em `http://localhost:8080/painel/login`.

## Testes

Para rodar os testes automatizados e gerar o relatório do JaCoCo:

```powershell
.\mvnw.cmd clean verify
```

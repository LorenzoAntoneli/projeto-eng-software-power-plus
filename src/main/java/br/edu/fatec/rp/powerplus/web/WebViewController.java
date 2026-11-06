package br.edu.fatec.rp.powerplus.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebViewController {

    @GetMapping("/agendar")
    public String agendar() {
        return "forward:/index.html";
    }

    @GetMapping("/comprovante")
    public String comprovante() {
        return "forward:/comprovante.html";
    }

    @GetMapping("/acompanhar")
    public String acompanhar() {
        return "forward:/acompanhar.html";
    }

    @GetMapping("/atendimento")
    public String atendimento() {
        return "forward:/atendimento.html";
    }

    @GetMapping({"/painel", "/painel/"})
    public String painelRaiz() {
        return "redirect:/painel/agenda";
    }

    @GetMapping("/painel/login")
    public String painelLogin() {
        return "forward:/painel/login.html";
    }

    @GetMapping("/painel/agenda")
    public String painelAgenda() {
        return "forward:/painel/agenda.html";
    }

    @GetMapping("/painel/expediente")
    public String painelExpediente() {
        return "forward:/painel/expediente.html";
    }

    @GetMapping("/painel/clientes")
    public String painelClientes() {
        return "forward:/painel/clientes.html";
    }

    @GetMapping("/painel/mensagens")
    public String painelMensagens() {
        return "forward:/painel/mensagens.html";
    }

    @GetMapping("/painel/avisos")
    public String painelAvisos() {
        return "forward:/painel/avisos.html";
    }
}


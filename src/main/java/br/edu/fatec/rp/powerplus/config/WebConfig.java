package br.edu.fatec.rp.powerplus.config;

import br.edu.fatec.rp.powerplus.service.AutenticacaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configura:
 * 1) A verificação automática de login nas rotas administrativas (/api/admin/**);
 * 2) Os endereços amigáveis das páginas HTML do site.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AutenticacaoService authService;

    public WebConfig(AutenticacaoService authService) {
        this.authService = authService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                var sessao = authService.validarToken(request.getHeader("Authorization"));
                request.setAttribute("responsavel", sessao.nomeExibicao());
                return true;
            }
        }).addPathPatterns("/api/admin/**").excludePathPatterns("/api/admin/auth/**");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/agendar").setViewName("forward:/index.html");
        registry.addViewController("/comprovante").setViewName("forward:/comprovante.html");
        registry.addViewController("/acompanhar").setViewName("forward:/acompanhar.html");
        registry.addViewController("/atendimento").setViewName("forward:/atendimento.html");

        registry.addRedirectViewController("/painel", "/painel/agenda");
        registry.addRedirectViewController("/painel/", "/painel/agenda");
        registry.addViewController("/painel/login").setViewName("forward:/painel/login.html");
        registry.addViewController("/painel/agenda").setViewName("forward:/painel/agenda.html");
        registry.addViewController("/painel/expediente").setViewName("forward:/painel/expediente.html");
        registry.addViewController("/painel/clientes").setViewName("forward:/painel/clientes.html");
        registry.addViewController("/painel/mensagens").setViewName("forward:/painel/mensagens.html");
        registry.addViewController("/painel/avisos").setViewName("forward:/painel/avisos.html");
    }
}

package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.RegraNegocioException;
import br.edu.fatec.rp.powerplus.domain.UsuarioProprietario;
import br.edu.fatec.rp.powerplus.repository.UsuarioProprietarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia o login do eletricista (senha criptografada com BCrypt e token de 8h).
 */
@Service
public class AutenticacaoService {

    public record SessaoAutenticada(String token, String username, String nomeExibicao, Instant expiraEm) {}

    private final UsuarioProprietarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final String defaultUsername;
    private final String defaultPassword;
    private final Map<String, SessaoAutenticada> sessoesAtivas = new ConcurrentHashMap<>();

    public AutenticacaoService(UsuarioProprietarioRepository usuarioRepository,
                               @Value("${powerplus.auth.owner-username:leonardo.powerplus}") String defaultUsername,
                               @Value("${powerplus.auth.owner-default-password:PowerPlus@2026}") String defaultPassword) {
        this.usuarioRepository = usuarioRepository;
        this.defaultUsername = defaultUsername;
        this.defaultPassword = defaultPassword;
    }

    @Transactional
    public void garantirUsuarioPadrao() {
        criarSeNaoExistir(defaultUsername, "Leonardo (Proprietário Power Plus)");
        if (!"carlos.powerplus".equalsIgnoreCase(defaultUsername)) {
            criarSeNaoExistir("carlos.powerplus", "Leonardo (Proprietário Power Plus)");
        }
    }

    private void criarSeNaoExistir(String username, String nomeExibicao) {
        if (usuarioRepository.findByUsernameIgnoreCase(username).isEmpty()) {
            usuarioRepository.save(new UsuarioProprietario(username, nomeExibicao, passwordEncoder.encode(defaultPassword)));
        }
    }

    @Transactional
    public SessaoAutenticada autenticar(String username, String senhaPura) {
        garantirUsuarioPadrao();
        if (username == null || senhaPura == null) {
            throw RegraNegocioException.naoAutorizado("Usuário e senha são obrigatórios.");
        }
        UsuarioProprietario usuario = usuarioRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> RegraNegocioException.naoAutorizado("Credenciais inválidas."));

        if (!passwordEncoder.matches(senhaPura, usuario.getSenhaHash())) {
            throw RegraNegocioException.naoAutorizado("Credenciais inválidas.");
        }

        String token = UUID.randomUUID().toString();
        SessaoAutenticada sessao = new SessaoAutenticada(
                token, usuario.getUsername(), usuario.getNomeExibicao(), Instant.now().plusSeconds(8 * 3600)
        );
        sessoesAtivas.put(token, sessao);
        return sessao;
    }

    public SessaoAutenticada validarToken(String tokenHeader) {
        if (tokenHeader == null || tokenHeader.isBlank()) {
            throw RegraNegocioException.naoAutorizado("Sessão não autenticada. Faça login como proprietário.");
        }
        String limpo = tokenHeader.replace("Bearer ", "").trim();
        SessaoAutenticada sessao = sessoesAtivas.get(limpo);
        if (sessao == null || sessao.expiraEm().isBefore(Instant.now())) {
            sessoesAtivas.remove(limpo);
            throw RegraNegocioException.naoAutorizado("Sessão expirada ou inválida. Faça login novamente.");
        }
        return sessao;
    }

    public void encerrarSessao(String tokenHeader) {
        if (tokenHeader != null) {
            sessoesAtivas.remove(tokenHeader.replace("Bearer ", "").trim());
        }
    }
}

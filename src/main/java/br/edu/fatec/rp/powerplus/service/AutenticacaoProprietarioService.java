package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.UsuarioProprietario;
import br.edu.fatec.rp.powerplus.domain.exception.NaoAutorizadoException;
import br.edu.fatec.rp.powerplus.repository.UsuarioProprietarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AutenticacaoProprietarioService {

    public record SessaoAutenticada(String token, String username, String nomeExibicao, Instant expiraEm) {}

    private final UsuarioProprietarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Clock clock;
    private final String defaultUsername;
    private final String defaultPassword;
    private final Map<String, SessaoAutenticada> sessoesAtivas = new ConcurrentHashMap<>();

    public AutenticacaoProprietarioService(UsuarioProprietarioRepository usuarioRepository,
                                           Clock clock,
                                           @Value("${powerplus.auth.owner-username:leonardo.powerplus}") String defaultUsername,
                                           @Value("${powerplus.auth.owner-default-password:PowerPlus@2026}") String defaultPassword) {
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
        this.defaultUsername = defaultUsername;
        this.defaultPassword = defaultPassword;
    }

    @Transactional
    public void garantirUsuarioPadrao() {
        garantirUsuario(defaultUsername, "Leonardo (Proprietário Power Plus)");
        if (!"carlos.powerplus".equalsIgnoreCase(defaultUsername)) {
            garantirUsuario("carlos.powerplus", "Leonardo (Proprietário Power Plus)");
        }
    }

    private void garantirUsuario(String username, String nomeExibicao) {
        if (usuarioRepository.findByUsernameIgnoreCase(username).isEmpty()) {
            String hash = passwordEncoder.encode(defaultPassword);
            usuarioRepository.save(new UsuarioProprietario(username, nomeExibicao, hash));
        }
    }

    @Transactional
    public SessaoAutenticada autenticar(String username, String senhaPura) {
        garantirUsuarioPadrao();
        if (username == null || senhaPura == null) {
            throw new NaoAutorizadoException("Usuário e senha são obrigatórios.");
        }
        UsuarioProprietario usuario = usuarioRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new NaoAutorizadoException("Credenciais inválidas."));

        if (!passwordEncoder.matches(senhaPura, usuario.getSenhaHash())) {
            throw new NaoAutorizadoException("Credenciais inválidas.");
        }

        String token = UUID.randomUUID().toString();
        Instant expira = Instant.now(clock).plusSeconds(8 * 3600);
        SessaoAutenticada sessao = new SessaoAutenticada(token, usuario.getUsername(), usuario.getNomeExibicao(), expira);
        sessoesAtivas.put(token, sessao);
        return sessao;
    }

    public SessaoAutenticada validarToken(String token) {
        if (token == null || token.isBlank()) {
            throw new NaoAutorizadoException("Sessão não autenticada. Faça login como proprietário.");
        }
        String limpo = token.replace("Bearer ", "").trim();
        SessaoAutenticada sessao = sessoesAtivas.get(limpo);
        if (sessao == null || sessao.expiraEm().isBefore(Instant.now(clock))) {
            sessoesAtivas.remove(limpo);
            throw new NaoAutorizadoException("Sessão expirada ou inválida. Faça login novamente.");
        }
        return sessao;
    }

    public void encerrarSessao(String token) {
        if (token != null) {
            sessoesAtivas.remove(token.replace("Bearer ", "").trim());
        }
    }
}

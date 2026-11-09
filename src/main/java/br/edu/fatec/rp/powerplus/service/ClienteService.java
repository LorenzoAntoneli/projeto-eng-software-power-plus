package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.Cliente;
import br.edu.fatec.rp.powerplus.domain.PerfilCliente;
import br.edu.fatec.rp.powerplus.domain.RegraNegocioException;
import br.edu.fatec.rp.powerplus.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Gerencia o cadastro, atualização e busca de clientes pelo nome ou telefone.
 */
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public Cliente cadastrarManual(String nome, String telefone, String email,
                                   String endereco, PerfilCliente perfil, String observacoes) {
        String telNormalizado = validarDadosObrigatorios(nome, telefone, endereco);
        if (clienteRepository.existsByTelefone(telNormalizado)) {
            throw RegraNegocioException.conflito("Já existe um cliente cadastrado com o telefone informado: " + telefone);
        }

        Cliente novo = new Cliente(nome.trim(), telNormalizado, endereco.trim(), perfil);
        novo.setEmail(limparTextoOpcional(email));
        novo.setObservacoes(limparTextoOpcional(observacoes));
        return clienteRepository.save(novo);
    }

    @Transactional
    public Cliente obterOuCadastrarNoAgendamento(String nome, String telefone,
                                                 String email, String endereco, PerfilCliente perfil) {
        String telNormalizado = validarDadosObrigatorios(nome, telefone, endereco);
        Cliente cliente = clienteRepository.findByTelefone(telNormalizado)
                .orElseGet(() -> new Cliente(nome.trim(), telNormalizado, endereco.trim(), perfil));

        cliente.setNome(nome.trim());
        cliente.setEnderecoPadrao(endereco.trim());
        if (perfil != null) {
            cliente.setPerfil(perfil);
        }
        if (email != null && !email.isBlank()) {
            cliente.setEmail(email.trim());
        }
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente atualizar(Long id, String nome, String telefone, String email,
                             String endereco, PerfilCliente perfil, String observacoes) {
        Cliente cliente = buscarPorId(id);
        String telNormalizado = validarDadosObrigatorios(nome, telefone, endereco);

        clienteRepository.findByTelefone(telNormalizado)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw RegraNegocioException.conflito("O telefone informado já pertence a outro cliente cadastrado: " + outro.getNome());
                });

        cliente.setNome(nome.trim());
        cliente.setTelefone(telNormalizado);
        cliente.setEmail(limparTextoOpcional(email));
        cliente.setEnderecoPadrao(endereco.trim());
        if (perfil != null) {
            cliente.setPerfil(perfil);
        }
        cliente.setObservacoes(limparTextoOpcional(observacoes));
        return clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Cliente não encontrado para o ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarOuPesquisar(String termo) {
        List<Cliente> todos = clienteRepository.findAllByOrderByNomeAsc();
        if (termo == null || termo.isBlank()) {
            return todos;
        }
        String buscaTexto = termo.trim().toLowerCase(Locale.ROOT);
        String buscaDigitos = Cliente.normalizarTelefone(termo);
        return todos.stream()
                .filter(c -> c.getNome().toLowerCase(Locale.ROOT).contains(buscaTexto)
                        || (!buscaDigitos.isEmpty() && c.getTelefone().contains(buscaDigitos)))
                .toList();
    }

    private String validarDadosObrigatorios(String nome, String telefone, String endereco) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome do cliente é obrigatório.");
        }
        String tel = Cliente.normalizarTelefone(telefone);
        if (tel.length() < 10 || tel.length() > 13) {
            throw new RegraNegocioException("Informe um telefone válido com DDD (10 ou 11 dígitos).");
        }
        if (endereco == null || endereco.isBlank()) {
            throw new RegraNegocioException("O endereço de atendimento é obrigatório.");
        }
        return tel;
    }

    private String limparTextoOpcional(String valor) {
        return (valor != null && !valor.isBlank()) ? valor.trim() : null;
    }
}

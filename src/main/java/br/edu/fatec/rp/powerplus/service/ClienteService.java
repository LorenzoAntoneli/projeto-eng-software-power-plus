package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.Cliente;
import br.edu.fatec.rp.powerplus.domain.PerfilCliente;
import br.edu.fatec.rp.powerplus.domain.exception.DuplicidadeCadastroException;
import br.edu.fatec.rp.powerplus.domain.exception.RecursoNaoEncontradoException;
import br.edu.fatec.rp.powerplus.domain.exception.RegraNegocioException;
import br.edu.fatec.rp.powerplus.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public Cliente cadastrarManual(String nome, String telefone, String email,
                                   String endereco, PerfilCliente perfil, String observacoes) {
        validarDadosObrigatorios(nome, telefone, endereco);
        String telNormalizado = Cliente.normalizarTelefone(telefone);

        if (clienteRepository.existsByTelefone(telNormalizado)) {
            throw new DuplicidadeCadastroException(
                    "Já existe um cliente cadastrado com o telefone informado: " + telefone);
        }

        Cliente novo = new Cliente(nome.trim(), telNormalizado, endereco.trim(), perfil);
        novo.setEmail(email != null && !email.isBlank() ? email.trim() : null);
        novo.setObservacoes(observacoes != null && !observacoes.isBlank() ? observacoes.trim() : null);
        return clienteRepository.save(novo);
    }

    @Transactional
    public Cliente obterOuCadastrarNoAgendamento(String nome, String telefone,
                                                 String email, String endereco, PerfilCliente perfil) {
        validarDadosObrigatorios(nome, telefone, endereco);
        String telNormalizado = Cliente.normalizarTelefone(telefone);

        Optional<Cliente> existente = clienteRepository.findByTelefone(telNormalizado);
        if (existente.isPresent()) {
            Cliente c = existente.get();
            c.setNome(nome.trim());
            c.setEnderecoPadrao(endereco.trim());
            if (perfil != null) {
                c.setPerfil(perfil);
            }
            if (email != null && !email.isBlank()) {
                c.setEmail(email.trim());
            }
            return clienteRepository.save(c);
        }

        Cliente novo = new Cliente(nome.trim(), telNormalizado, endereco.trim(), perfil);
        if (email != null && !email.isBlank()) {
            novo.setEmail(email.trim());
        }
        return clienteRepository.save(novo);
    }

    @Transactional
    public Cliente atualizar(Long id, String nome, String telefone, String email,
                             String endereco, PerfilCliente perfil, String observacoes) {
        Cliente cliente = buscarPorId(id);
        validarDadosObrigatorios(nome, telefone, endereco);
        String telNormalizado = Cliente.normalizarTelefone(telefone);

        clienteRepository.findByTelefone(telNormalizado)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new DuplicidadeCadastroException(
                            "O telefone informado já pertence a outro cliente cadastrado: " + outro.getNome());
                });

        cliente.setNome(nome.trim());
        cliente.setTelefone(telNormalizado);
        cliente.setEmail(email != null && !email.isBlank() ? email.trim() : null);
        cliente.setEnderecoPadrao(endereco.trim());
        if (perfil != null) {
            cliente.setPerfil(perfil);
        }
        cliente.setObservacoes(observacoes != null && !observacoes.isBlank() ? observacoes.trim() : null);
        return clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado para o ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarOuPesquisar(String termo) {
        if (termo == null || termo.isBlank()) {
            return clienteRepository.findAllByOrderByNomeAsc();
        }
        String limpo = termo.trim();
        String numerico = Cliente.normalizarTelefone(limpo);
        if (numerico.isEmpty()) {
            numerico = "__SEM_NUMERO__";
        }
        return clienteRepository.pesquisarPorNomeOuTelefone(limpo, numerico);
    }

    private void validarDadosObrigatorios(String nome, String telefone, String endereco) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome do cliente é obrigatório.");
        }
        String telNormalizado = Cliente.normalizarTelefone(telefone);
        if (telNormalizado.length() < 10 || telNormalizado.length() > 13) {
            throw new RegraNegocioException("Informe um telefone válido com DDD (10 ou 11 dígitos).");
        }
        if (endereco == null || endereco.isBlank()) {
            throw new RegraNegocioException("O endereço de atendimento é obrigatório.");
        }
    }
}

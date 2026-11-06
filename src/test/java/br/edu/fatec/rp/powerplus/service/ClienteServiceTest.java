package br.edu.fatec.rp.powerplus.service;

import br.edu.fatec.rp.powerplus.domain.Cliente;
import br.edu.fatec.rp.powerplus.domain.PerfilCliente;
import br.edu.fatec.rp.powerplus.domain.exception.DuplicidadeCadastroException;
import br.edu.fatec.rp.powerplus.domain.exception.RegraNegocioException;
import br.edu.fatec.rp.powerplus.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    @DisplayName("TU01 - Deve cadastrar cliente normalizando máscara do telefone (RF01/HU09)")
    void deveCadastrarClienteNormalizandoTelefone() {
        when(clienteRepository.existsByTelefone("16991234567")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente salvo = clienteService.cadastrarManual(
                "Mariana Ferreira",
                "(16) 99123-4567",
                "mariana@email.com",
                "Rua São Sebastião, 410 - Centro, Ribeirão Preto",
                PerfilCliente.RESIDENCIAL,
                "Portão branco"
        );

        assertEquals("Mariana Ferreira", salvo.getNome());
        assertEquals("16991234567", salvo.getTelefone());
        assertEquals(PerfilCliente.RESIDENCIAL, salvo.getPerfil());
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    @DisplayName("TU02 - Deve impedir cadastro manual com telefone duplicado (RN05/UC06)")
    void deveImpedirCadastroManualComTelefoneDuplicado() {
        when(clienteRepository.existsByTelefone("16991234567")).thenReturn(true);

        assertThrows(DuplicidadeCadastroException.class, () -> clienteService.cadastrarManual(
                "Outro Nome",
                "(16) 99123-4567",
                null,
                "Av. Independência, 1200",
                PerfilCliente.RESIDENCIAL,
                null
        ));

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("TU03 - Deve reaproveitar cadastro existente no autoatendimento de agendamento (UC01/UC06)")
    void deveReaproveitarClienteExistenteNoAgendamento() {
        Cliente existente = new Cliente("Condomínio Solar", "1636201122", "Av. Antiga, 10", PerfilCliente.EMPRESARIAL_CONDOMINIO);
        existente.setId(7L);
        when(clienteRepository.findByTelefone("1636201122")).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente atualizado = clienteService.obterOuCadastrarNoAgendamento(
                "Condomínio Solar das Palmeiras",
                "(16) 3620-1122",
                "sindico@solar.com.br",
                "Av. João Fiúsa, 950 - Jardim Irajá",
                PerfilCliente.EMPRESARIAL_CONDOMINIO
        );

        assertEquals(7L, atualizado.getId());
        assertEquals("Condomínio Solar das Palmeiras", atualizado.getNome());
        assertEquals("Av. João Fiúsa, 950 - Jardim Irajá", atualizado.getEnderecoPadrao());
    }

    @Test
    @DisplayName("TU04 - Deve rejeitar cliente com telefone inválido ou endereço em branco (Teste Negativo)")
    void deveRejeitarTelefoneInvalidoOuEnderecoVazio() {
        assertThrows(RegraNegocioException.class, () -> clienteService.cadastrarManual(
                "Cliente Teste", "12345", null, "Rua A, 10", PerfilCliente.RESIDENCIAL, null
        ));
        assertThrows(RegraNegocioException.class, () -> clienteService.cadastrarManual(
                "Cliente Teste", "(16) 99111-2233", null, "   ", PerfilCliente.RESIDENCIAL, null
        ));
    }
}

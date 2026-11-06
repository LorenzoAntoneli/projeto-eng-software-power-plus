package br.edu.fatec.rp.powerplus.config;

import br.edu.fatec.rp.powerplus.repository.AgendamentoRepository;
import br.edu.fatec.rp.powerplus.repository.ClienteRepository;
import br.edu.fatec.rp.powerplus.repository.ConversaChatRepository;
import br.edu.fatec.rp.powerplus.repository.DisponibilidadeSemanalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:powerplus_seed_test;DB_CLOSE_DELAY=-1",
        "powerplus.seed.enabled=true"
})
@ActiveProfiles("test")
@DirtiesContext
class DataSeederIntegrationTest {

    @Autowired
    private DisponibilidadeSemanalRepository disponibilidadeRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private ConversaChatRepository conversaChatRepository;

    @Test
    @DisplayName("TI07 - Deve popular dados iniciais fictícios/anonimizados para demonstração do MVP")
    void devePopularDadosDemonstracaoQuandoHabilitado() {
        assertTrue(disponibilidadeRepository.count() >= 7);
        assertTrue(clienteRepository.count() >= 4);
        assertTrue(agendamentoRepository.count() >= 4);
        assertTrue(conversaChatRepository.count() >= 1);
    }
}

package br.edu.fatec.rp.powerplus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    public static final ZoneId FUSO_PADRAO = ZoneId.of("America/Sao_Paulo");

    @Bean
    public Clock systemClock() {
        return Clock.system(FUSO_PADRAO);
    }
}

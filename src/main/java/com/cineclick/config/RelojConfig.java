package com.cineclick.config;

import com.cineclick.service.MetricasService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.system(MetricasService.ZONA);
    }
}

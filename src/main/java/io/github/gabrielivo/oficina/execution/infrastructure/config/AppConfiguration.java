package io.github.gabrielivo.oficina.execution.infrastructure.config;

import io.github.gabrielivo.oficina.execution.application.execucao.SimulacaoProperties;
import io.github.gabrielivo.oficina.execution.application.mensageria.MensageriaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({MensageriaProperties.class, SimulacaoProperties.class})
public class AppConfiguration {}

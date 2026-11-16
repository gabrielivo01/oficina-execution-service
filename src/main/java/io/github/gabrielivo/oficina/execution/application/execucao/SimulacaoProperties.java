package io.github.gabrielivo.oficina.execution.application.execucao;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.simulacao")
public record SimulacaoProperties(boolean recusarInicioExecucao) {}

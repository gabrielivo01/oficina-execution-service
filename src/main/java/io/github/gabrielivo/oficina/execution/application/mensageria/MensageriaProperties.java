package io.github.gabrielivo.oficina.execution.application.mensageria;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mensageria")
public record MensageriaProperties(
        String exchangeComandos, String exchangeEventos, String filaComandos, Outbox outbox) {

    public record Outbox(boolean publicadorHabilitado, Duration intervalo, int lote, Duration timeoutConfirmacao) {}
}

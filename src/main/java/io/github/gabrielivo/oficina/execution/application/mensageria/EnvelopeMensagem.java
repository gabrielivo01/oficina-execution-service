package io.github.gabrielivo.oficina.execution.application.mensageria;

import java.time.Instant;
import java.util.Map;

public record EnvelopeMensagem(
        String messageId, String type, String sagaId, String osId, Instant occurredAt, Map<String, Object> payload) {}

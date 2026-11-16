package io.github.gabrielivo.oficina.execution.application.mensageria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.execution.domain.mensageria.MensagemOutbox;
import io.github.gabrielivo.oficina.execution.domain.mensageria.MensagemOutboxRepository;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private MensagemOutboxRepository repository;

    @Mock
    private ContextoRastreamento contextoRastreamento;

    @Test
    void publicaEventosComRoutingKeyExecutionEmKebabCase() {
        MensageriaProperties properties = new MensageriaProperties(
                "oficina.comandos",
                "oficina.eventos",
                "execution.comandos",
                new MensageriaProperties.Outbox(true, Duration.ofSeconds(1), 50, Duration.ofSeconds(5)));
        when(contextoRastreamento.traceParentAtual()).thenReturn(null);
        OutboxService service =
                new OutboxService(repository, properties, JsonMapper.builder().build(), contextoRastreamento);

        service.registrar("ExecucaoCancelada", "os-1", Map.of("reservaId", "reserva-1"));

        ArgumentCaptor<MensagemOutbox> captor = ArgumentCaptor.forClass(MensagemOutbox.class);
        verify(repository).save(captor.capture());
        assertEquals("execution.execucao-cancelada", captor.getValue().getRoutingKey());
        assertEquals("oficina.eventos", captor.getValue().getExchange());
    }
}

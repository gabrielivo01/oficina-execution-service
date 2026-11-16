package io.github.gabrielivo.oficina.execution.infrastructure.mensageria;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.execution.application.mensageria.MensageriaProperties;
import io.github.gabrielivo.oficina.execution.application.mensageria.OutboxService;
import io.github.gabrielivo.oficina.execution.domain.mensageria.MensagemOutbox;
import io.github.gabrielivo.oficina.execution.domain.mensageria.MensagemOutboxRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class PublicadorOutboxTest {

    @Mock
    private MensagemOutboxRepository repository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private PublicadorOutbox publicador;

    @BeforeEach
    void setUp() {
        publicador = new PublicadorOutbox(
                repository,
                outboxService,
                rabbitTemplate,
                new MensageriaProperties(
                        "commands",
                        "events",
                        "commands.queue",
                        new MensageriaProperties.Outbox(true, Duration.ofSeconds(1), 10, Duration.ofMillis(20))));
    }

    @Test
    void confirmaMensagemEPropagaTraceparent() throws Exception {
        MensagemOutbox mensagem = mensagem("00-0123456789abcdef0123456789abcdef-0123456789abcdef-01");
        when(repository.findByEnviadoEmIsNullOrderByCriadoEmAsc(any())).thenReturn(List.of(mensagem));
        doAnswer(invocation -> {
                    MessagePostProcessor postProcessor = invocation.getArgument(3);
                    CorrelationData correlation = invocation.getArgument(4);
                    Message transformed =
                            postProcessor.postProcessMessage(new Message("{}".getBytes(), new MessageProperties()));
                    assertTrue(transformed.getMessageProperties().getHeaders().containsKey("traceparent"));
                    correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
                    return null;
                })
                .when(rabbitTemplate)
                .convertAndSend(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(MessagePostProcessor.class),
                        any(CorrelationData.class));

        publicador.publicarPendentes();

        verify(outboxService).marcarEnviada(mensagem.getId());
    }

    @Test
    void naoPublicaQuandoPublicadorEstaDesabilitado() {
        publicador = new PublicadorOutbox(
                repository,
                outboxService,
                rabbitTemplate,
                new MensageriaProperties(
                        "commands",
                        "events",
                        "commands.queue",
                        new MensageriaProperties.Outbox(false, Duration.ofSeconds(1), 10, Duration.ofMillis(20))));

        publicador.publicarPendentes();

        verify(repository, never()).findByEnviadoEmIsNullOrderByCriadoEmAsc(any());
        verifyNoInteractions(rabbitTemplate, outboxService);
    }

    @Test
    void registraFalhaQuandoBrokerRejeitaOuDevolveMensagem() {
        MensagemOutbox mensagem = mensagem(null);
        when(repository.findByEnviadoEmIsNullOrderByCriadoEmAsc(any())).thenReturn(List.of(mensagem));
        doAnswer(invocation -> {
                    CorrelationData correlation = invocation.getArgument(4);
                    correlation.getFuture().complete(new CorrelationData.Confirm(false, "nack"));
                    return null;
                })
                .when(rabbitTemplate)
                .convertAndSend(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(MessagePostProcessor.class),
                        any(CorrelationData.class));

        publicador.publicarPendentes();

        verify(outboxService).registrarFalha(mensagem.getId());
        verify(outboxService, never()).marcarEnviada(anyString());

        MensagemOutbox devolvida = mensagem(null);
        when(repository.findByEnviadoEmIsNullOrderByCriadoEmAsc(any())).thenReturn(List.of(devolvida));
        doAnswer(invocation -> {
                    CorrelationData correlation = invocation.getArgument(4);
                    correlation.setReturned(org.mockito.Mockito.mock(ReturnedMessage.class));
                    correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
                    return null;
                })
                .when(rabbitTemplate)
                .convertAndSend(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(MessagePostProcessor.class),
                        any(CorrelationData.class));

        publicador.publicarPendentes();

        verify(outboxService).registrarFalha(devolvida.getId());
    }

    @Test
    void registraFalhaEmErroDoClienteTimeoutEInterrupcao() throws Exception {
        MensagemOutbox mensagem = mensagem(null);
        when(repository.findByEnviadoEmIsNullOrderByCriadoEmAsc(any())).thenReturn(List.of(mensagem));
        doThrow(new AmqpException("broker indisponível"))
                .doNothing()
                .doNothing()
                .when(rabbitTemplate)
                .convertAndSend(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(MessagePostProcessor.class),
                        any(CorrelationData.class));

        publicador.publicarPendentes();
        verify(outboxService).registrarFalha(mensagem.getId());

        MensagemOutbox timeout = mensagem(null);
        when(repository.findByEnviadoEmIsNullOrderByCriadoEmAsc(any())).thenReturn(List.of(timeout));
        publicador.publicarPendentes();
        verify(outboxService).registrarFalha(timeout.getId());

        MensagemOutbox interrompida = mensagem(null);
        when(repository.findByEnviadoEmIsNullOrderByCriadoEmAsc(any())).thenReturn(List.of(interrompida));
        Thread.currentThread().interrupt();
        try {
            publicador.publicarPendentes();
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        verify(outboxService).registrarFalha(interrompida.getId());
    }

    private MensagemOutbox mensagem(String traceParent) {
        return new MensagemOutbox("Evento", "events", "execution.evento", "{}", traceParent);
    }
}

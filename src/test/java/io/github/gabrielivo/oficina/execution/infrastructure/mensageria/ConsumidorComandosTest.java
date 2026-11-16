package io.github.gabrielivo.oficina.execution.infrastructure.mensageria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.gabrielivo.oficina.execution.application.mensageria.EnvelopeMensagem;
import io.github.gabrielivo.oficina.execution.application.mensageria.ProcessadorComandosService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ConsumidorComandosTest {

    private static final String COMANDO = """
            {"messageId":"m-1","type":"LiberarReservaPecas","sagaId":"os-1","osId":"os-1",
             "occurredAt":"2026-11-15T15:00:00Z","payload":{"reservaId":"r-1","motivo":"ORCAMENTO_RECUSADO"}}
            """;

    @Mock
    private ProcessadorComandosService processador;

    private ConsumidorComandos consumidor;

    @BeforeEach
    void setUp() {
        consumidor = new ConsumidorComandos(processador, JsonMapper.builder().build());
    }

    @Test
    void deveDesserializarOEnvelopeEDelegarAoProcessador() {
        consumidor.receber(mensagem(COMANDO));

        ArgumentCaptor<EnvelopeMensagem> captor = ArgumentCaptor.forClass(EnvelopeMensagem.class);
        verify(processador).processar(captor.capture());
        EnvelopeMensagem envelope = captor.getValue();
        assertThat(envelope.messageId()).isEqualTo("m-1");
        assertThat(envelope.type()).isEqualTo("LiberarReservaPecas");
        assertThat(envelope.osId()).isEqualTo("os-1");
        assertThat(envelope.payload()).containsEntry("reservaId", "r-1");
    }

    @Test
    void jsonInvalidoDeveIrDiretoParaADlqSemRetentativa() {
        assertThatThrownBy(() -> consumidor.receber(mensagem("{nao é json")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageStartingWith("Comando inválido");
        verifyNoInteractions(processador);
    }

    @Test
    void comandoComDadosInvalidosDeveIrParaADlq() {
        doThrow(new IllegalArgumentException("osId e payload são obrigatórios."))
                .when(processador)
                .processar(any());

        assertThatThrownBy(() -> consumidor.receber(mensagem(COMANDO)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("osId e payload são obrigatórios.");
    }

    @Test
    void tipoNaoSuportadoDeveIrParaADlq() {
        doThrow(new ProcessadorComandosService.MensagemNaoSuportadaException("ComandoDesconhecido"))
                .when(processador)
                .processar(any());

        assertThatThrownBy(() -> consumidor.receber(mensagem(COMANDO)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void falhaTransitoriaDevePropagarParaORetryDoListener() {
        doThrow(new IllegalStateException("banco indisponível"))
                .when(processador)
                .processar(any());

        assertThatThrownBy(() -> consumidor.receber(mensagem(COMANDO)))
                .isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    private static Message mensagem(String corpo) {
        return new Message(corpo.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}

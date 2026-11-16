package io.github.gabrielivo.oficina.execution.infrastructure.mensageria;

import io.github.gabrielivo.oficina.execution.application.mensageria.EnvelopeMensagem;
import io.github.gabrielivo.oficina.execution.application.mensageria.ProcessadorComandosService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ConsumidorComandos {

    private final ProcessadorComandosService processador;
    private final JsonMapper jsonMapper;

    public ConsumidorComandos(ProcessadorComandosService processador, JsonMapper jsonMapper) {
        this.processador = processador;
        this.jsonMapper = jsonMapper;
    }

    @RabbitListener(queues = "${app.mensageria.fila-comandos:execution.comandos}")
    public void receber(Message mensagem) {
        try {
            processador.processar(jsonMapper.readValue(mensagem.getBody(), EnvelopeMensagem.class));
        } catch (JacksonException | IllegalArgumentException e) {
            throw new AmqpRejectAndDontRequeueException("Comando inválido: " + e.getMessage(), e);
        } catch (ProcessadorComandosService.MensagemNaoSuportadaException e) {
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
    }
}

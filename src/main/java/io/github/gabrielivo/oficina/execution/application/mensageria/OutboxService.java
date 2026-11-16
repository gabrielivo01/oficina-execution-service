package io.github.gabrielivo.oficina.execution.application.mensageria;

import io.github.gabrielivo.oficina.execution.domain.mensageria.MensagemOutbox;
import io.github.gabrielivo.oficina.execution.domain.mensageria.MensagemOutboxRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class OutboxService {

    private final MensagemOutboxRepository repository;
    private final MensageriaProperties properties;
    private final JsonMapper jsonMapper;
    private final ContextoRastreamento contextoRastreamento;

    public OutboxService(
            MensagemOutboxRepository repository,
            MensageriaProperties properties,
            JsonMapper jsonMapper,
            ContextoRastreamento contextoRastreamento) {
        this.repository = repository;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.contextoRastreamento = contextoRastreamento;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String tipo, String osId, Map<String, Object> payload) {
        var envelope = new EnvelopeMensagem(UUID.randomUUID().toString(), tipo, osId, osId, Instant.now(), payload);
        try {
            repository.save(new MensagemOutbox(
                    tipo,
                    properties.exchangeEventos(),
                    routingKey(tipo),
                    jsonMapper.writeValueAsString(envelope),
                    contextoRastreamento.traceParentAtual()));
        } catch (JacksonException e) {
            throw new IllegalStateException("Não foi possível serializar o evento " + tipo + ".", e);
        }
    }

    private String routingKey(String tipo) {
        StringBuilder key = new StringBuilder("execution.");
        for (char character : tipo.toCharArray()) {
            if (Character.isUpperCase(character) && key.length() > "execution.".length()) {
                key.append('-');
            }
            key.append(Character.toLowerCase(character));
        }
        return key.toString();
    }

    @Transactional
    public void marcarEnviada(String id) {
        buscar(id).marcarEnviada();
    }

    @Transactional
    public void registrarFalha(String id) {
        buscar(id).registrarFalha();
    }

    private MensagemOutbox buscar(String id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new IllegalStateException("Mensagem outbox não encontrada: " + id));
    }
}

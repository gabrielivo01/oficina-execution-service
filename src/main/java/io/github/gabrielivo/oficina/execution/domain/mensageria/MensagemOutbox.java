package io.github.gabrielivo.oficina.execution.domain.mensageria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox")
public class MensagemOutbox {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(nullable = false, length = 150)
    private String exchange;

    @Column(name = "routing_key", nullable = false, length = 150)
    private String routingKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "trace_parent", length = 100)
    private String traceParent;

    protected MensagemOutbox() {}

    public MensagemOutbox(String tipo, String exchange, String routingKey, String payload, String traceParent) {
        this.id = UUID.randomUUID().toString();
        this.tipo = tipo;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.payload = payload;
        this.traceParent = traceParent;
        this.criadoEm = LocalDateTime.now();
    }

    public void marcarEnviada() {
        enviadoEm = LocalDateTime.now();
    }

    public void registrarFalha() {
        tentativas++;
    }

    public String getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getPayload() {
        return payload;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getEnviadoEm() {
        return enviadoEm;
    }

    public int getTentativas() {
        return tentativas;
    }

    public String getTraceParent() {
        return traceParent;
    }
}

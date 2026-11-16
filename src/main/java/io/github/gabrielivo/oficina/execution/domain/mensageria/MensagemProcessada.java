package io.github.gabrielivo.oficina.execution.domain.mensageria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "inbox")
public class MensagemProcessada {

    @Id
    @Column(name = "message_id", length = 36)
    private String messageId;

    @Column(name = "processado_em", nullable = false)
    private LocalDateTime processadoEm;

    protected MensagemProcessada() {}

    public MensagemProcessada(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("messageId é obrigatório.");
        }
        this.messageId = messageId;
        this.processadoEm = LocalDateTime.now();
    }
}

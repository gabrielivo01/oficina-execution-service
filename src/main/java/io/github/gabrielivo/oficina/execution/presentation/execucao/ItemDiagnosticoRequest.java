package io.github.gabrielivo.oficina.execution.presentation.execucao;

import io.github.gabrielivo.oficina.execution.domain.execucao.TipoItemDiagnostico;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ItemDiagnosticoRequest(
        @NotNull TipoItemDiagnostico tipo,
        @NotBlank @Size(max = 150) String descricao,
        @PositiveOrZero BigDecimal valorUnitario,
        String pecaId,
        @Positive int quantidade) {}

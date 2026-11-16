package io.github.gabrielivo.oficina.execution.presentation.peca;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PecaRequest(
        @NotBlank @Size(max = 150) String nome,
        String descricao,
        @NotNull @Positive BigDecimal preco,
        @PositiveOrZero int quantidadeEstoque,
        @PositiveOrZero int estoqueMinimo) {}

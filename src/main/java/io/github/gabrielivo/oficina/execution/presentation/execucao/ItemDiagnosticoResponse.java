package io.github.gabrielivo.oficina.execution.presentation.execucao;

import io.github.gabrielivo.oficina.execution.domain.execucao.TipoItemDiagnostico;
import java.math.BigDecimal;

public record ItemDiagnosticoResponse(
        TipoItemDiagnostico tipo,
        String descricao,
        String pecaId,
        int quantidade,
        BigDecimal valorUnitario,
        BigDecimal valorTotal) {}

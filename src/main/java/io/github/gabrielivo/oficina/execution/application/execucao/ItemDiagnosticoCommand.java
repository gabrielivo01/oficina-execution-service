package io.github.gabrielivo.oficina.execution.application.execucao;

import io.github.gabrielivo.oficina.execution.domain.execucao.TipoItemDiagnostico;
import java.math.BigDecimal;

public record ItemDiagnosticoCommand(
        TipoItemDiagnostico tipo, String descricao, BigDecimal valorUnitario, String pecaId, int quantidade) {}

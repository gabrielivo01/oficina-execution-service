package io.github.gabrielivo.oficina.execution.presentation.execucao;

import io.github.gabrielivo.oficina.execution.domain.execucao.StatusTarefa;
import io.github.gabrielivo.oficina.execution.domain.execucao.TipoTarefa;
import java.time.LocalDateTime;
import java.util.List;

public record TarefaResponse(
        String id,
        String osId,
        TipoTarefa tipo,
        StatusTarefa status,
        String placa,
        String observacao,
        int prioridade,
        String progresso,
        List<ItemDiagnosticoResponse> itens,
        LocalDateTime criadoEm,
        LocalDateTime iniciadoEm,
        LocalDateTime concluidoEm) {}

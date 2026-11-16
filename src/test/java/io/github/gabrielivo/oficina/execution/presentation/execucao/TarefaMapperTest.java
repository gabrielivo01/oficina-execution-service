package io.github.gabrielivo.oficina.execution.presentation.execucao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.gabrielivo.oficina.execution.domain.execucao.ItemDiagnostico;
import io.github.gabrielivo.oficina.execution.domain.execucao.TarefaExecucao;
import io.github.gabrielivo.oficina.execution.domain.execucao.TipoItemDiagnostico;
import io.github.gabrielivo.oficina.execution.domain.execucao.TipoTarefa;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class TarefaMapperTest {

    private final TarefaMapper mapper = new TarefaMapper();

    @Test
    void converteItensDaRequisicaoParaCommands() {
        var request = new ConcluirDiagnosticoRequest(List.of(new ItemDiagnosticoRequest(
                TipoItemDiagnostico.SERVICO, "Diagnóstico", new BigDecimal("25.00"), null, 1)));

        var commands = mapper.paraCommands(request);

        assertEquals(1, commands.size());
        assertEquals("Diagnóstico", commands.getFirst().descricao());
        assertEquals(TipoItemDiagnostico.SERVICO, commands.getFirst().tipo());
    }

    @Test
    void respostaIncluiTarefaEItensDoDiagnostico() {
        TarefaExecucao tarefa = new TarefaExecucao("os-1", TipoTarefa.DIAGNOSTICO, "ABC1D23", "ruído", 2);
        tarefa.iniciar();
        tarefa.concluirDiagnostico(List.of(
                new ItemDiagnostico(TipoItemDiagnostico.SERVICO, "Diagnóstico", new BigDecimal("25.00"), null, 1)));

        TarefaResponse response = mapper.paraResponse(tarefa);

        assertEquals(tarefa.getId(), response.id());
        assertEquals(TipoTarefa.DIAGNOSTICO, response.tipo());
        assertEquals("Diagnóstico", response.itens().getFirst().descricao());
        assertEquals(new BigDecimal("25.00"), response.itens().getFirst().valorTotal());
    }
}

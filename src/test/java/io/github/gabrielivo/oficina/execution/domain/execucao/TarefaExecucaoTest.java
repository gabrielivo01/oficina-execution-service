package io.github.gabrielivo.oficina.execution.domain.execucao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class TarefaExecucaoTest {

    @Test
    void deveConcluirDiagnosticoComSnapshotDosItens() {
        TarefaExecucao tarefa = new TarefaExecucao("os-1", TipoTarefa.DIAGNOSTICO, "ABC1D23", "Ruído", 2);
        tarefa.iniciar();
        ItemDiagnostico item =
                new ItemDiagnostico(TipoItemDiagnostico.SERVICO, "Inspeção", new BigDecimal("25.00"), null, 1);

        tarefa.concluirDiagnostico(List.of(item));

        assertEquals(StatusTarefa.CONCLUIDA, tarefa.getStatus());
        assertEquals(new BigDecimal("25.00"), tarefa.getItens().getFirst().getValorTotal());
        assertThrows(
                UnsupportedOperationException.class, () -> tarefa.getItens().clear());
        assertThrows(IllegalStateException.class, tarefa::iniciar);
    }

    @Test
    void deveRegistrarProgressoEFinalizarSomenteReparoEmAndamento() {
        TarefaExecucao tarefa = new TarefaExecucao("os-2", TipoTarefa.REPARO, "ABC1D23", null, 0);

        assertThrows(IllegalStateException.class, tarefa::finalizarReparo);
        tarefa.iniciar();
        tarefa.registrarProgresso("Desmontagem iniciada");
        tarefa.finalizarReparo();

        assertEquals(StatusTarefa.CONCLUIDA, tarefa.getStatus());
        assertEquals(1, tarefa.getProgresso().lines().count());
    }

    @Test
    void deveExigirQuantidadeUmParaServico() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ItemDiagnostico(TipoItemDiagnostico.SERVICO, "Inspeção", BigDecimal.TEN, null, 2));
    }
}

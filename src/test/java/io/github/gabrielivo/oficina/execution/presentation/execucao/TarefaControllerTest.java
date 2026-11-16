package io.github.gabrielivo.oficina.execution.presentation.execucao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.execution.application.execucao.ItemDiagnosticoCommand;
import io.github.gabrielivo.oficina.execution.application.execucao.TarefaExecucaoService;
import io.github.gabrielivo.oficina.execution.domain.execucao.StatusTarefa;
import io.github.gabrielivo.oficina.execution.domain.execucao.TarefaExecucao;
import io.github.gabrielivo.oficina.execution.domain.execucao.TipoItemDiagnostico;
import io.github.gabrielivo.oficina.execution.domain.execucao.TipoTarefa;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TarefaControllerTest {

    @Mock
    private TarefaExecucaoService service;

    @Mock
    private TarefaMapper mapper;

    private TarefaController controller;
    private TarefaExecucao tarefa;
    private TarefaResponse response;

    @BeforeEach
    void setUp() {
        controller = new TarefaController(service, mapper);
        tarefa = new TarefaExecucao("os-1", TipoTarefa.DIAGNOSTICO, "ABC1D23", null, 1);
        response = new TarefaResponse(
                tarefa.getId(),
                tarefa.getOsId(),
                tarefa.getTipo(),
                tarefa.getStatus(),
                tarefa.getPlaca(),
                tarefa.getObservacao(),
                tarefa.getPrioridade(),
                tarefa.getProgresso(),
                List.of(),
                tarefa.getCriadoEm(),
                tarefa.getIniciadoEm(),
                tarefa.getConcluidoEm());
    }

    @Test
    void listaEBuscaTarefas() {
        when(service.listar(StatusTarefa.NA_FILA)).thenReturn(List.of(tarefa));
        when(service.buscar(tarefa.getId())).thenReturn(tarefa);
        when(mapper.paraResponse(tarefa)).thenReturn(response);

        assertEquals(List.of(response), controller.listar(StatusTarefa.NA_FILA));
        assertEquals(response, controller.buscar(tarefa.getId()));
    }

    @Test
    void delegaAcoesDoMecanicoEConverteDiagnostico() {
        when(service.iniciarDiagnostico(tarefa.getId())).thenReturn(tarefa);
        when(service.concluirDiagnostico(
                        org.mockito.ArgumentMatchers.eq(tarefa.getId()), org.mockito.ArgumentMatchers.any()))
                .thenReturn(tarefa);
        when(service.iniciarReparoDaFila(tarefa.getId())).thenReturn(tarefa);
        when(service.registrarProgresso(tarefa.getId(), "teste")).thenReturn(tarefa);
        when(service.finalizarReparo(tarefa.getId())).thenReturn(tarefa);
        when(mapper.paraResponse(tarefa)).thenReturn(response);
        var item = new ItemDiagnosticoRequest(TipoItemDiagnostico.SERVICO, "Serviço", new BigDecimal("5.00"), null, 1);
        var request = new ConcluirDiagnosticoRequest(List.of(item));
        when(mapper.paraCommands(request))
                .thenReturn(List.of(new ItemDiagnosticoCommand(
                        TipoItemDiagnostico.SERVICO, "Serviço", new BigDecimal("5.00"), null, 1)));

        assertEquals(response, controller.iniciarDiagnostico(tarefa.getId()));
        assertEquals(response, controller.concluirDiagnostico(tarefa.getId(), request));
        assertEquals(response, controller.iniciarReparo(tarefa.getId()));
        assertEquals(response, controller.registrarProgresso(tarefa.getId(), new ProgressoRequest("teste")));
        assertEquals(response, controller.finalizar(tarefa.getId()));
        verify(service)
                .concluirDiagnostico(
                        org.mockito.ArgumentMatchers.eq(tarefa.getId()), org.mockito.ArgumentMatchers.any());
    }
}

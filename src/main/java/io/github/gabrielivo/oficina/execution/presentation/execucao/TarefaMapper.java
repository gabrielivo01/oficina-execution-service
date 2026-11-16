package io.github.gabrielivo.oficina.execution.presentation.execucao;

import io.github.gabrielivo.oficina.execution.application.execucao.ItemDiagnosticoCommand;
import io.github.gabrielivo.oficina.execution.domain.execucao.ItemDiagnostico;
import io.github.gabrielivo.oficina.execution.domain.execucao.TarefaExecucao;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TarefaMapper {

    public List<ItemDiagnosticoCommand> paraCommands(ConcluirDiagnosticoRequest request) {
        return request.itens().stream()
                .map(item -> new ItemDiagnosticoCommand(
                        item.tipo(), item.descricao(), item.valorUnitario(), item.pecaId(), item.quantidade()))
                .toList();
    }

    public TarefaResponse paraResponse(TarefaExecucao tarefa) {
        List<ItemDiagnosticoResponse> itens =
                tarefa.getItens().stream().map(this::paraItemResponse).toList();
        return new TarefaResponse(
                tarefa.getId(),
                tarefa.getOsId(),
                tarefa.getTipo(),
                tarefa.getStatus(),
                tarefa.getPlaca(),
                tarefa.getObservacao(),
                tarefa.getPrioridade(),
                tarefa.getProgresso(),
                itens,
                tarefa.getCriadoEm(),
                tarefa.getIniciadoEm(),
                tarefa.getConcluidoEm());
    }

    private ItemDiagnosticoResponse paraItemResponse(ItemDiagnostico item) {
        return new ItemDiagnosticoResponse(
                item.getTipo(),
                item.getDescricao(),
                item.getPecaId(),
                item.getQuantidade(),
                item.getValorUnitario(),
                item.getValorTotal());
    }
}

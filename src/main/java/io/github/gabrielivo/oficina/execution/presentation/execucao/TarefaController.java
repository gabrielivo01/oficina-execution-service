package io.github.gabrielivo.oficina.execution.presentation.execucao;

import io.github.gabrielivo.oficina.execution.application.execucao.TarefaExecucaoService;
import io.github.gabrielivo.oficina.execution.domain.execucao.StatusTarefa;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tarefas")
public class TarefaController {

    private final TarefaExecucaoService service;
    private final TarefaMapper mapper;

    public TarefaController(TarefaExecucaoService service, TarefaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<TarefaResponse> listar(@RequestParam(defaultValue = "NA_FILA") StatusTarefa status) {
        return service.listar(status).stream().map(mapper::paraResponse).toList();
    }

    @GetMapping("/{id}")
    public TarefaResponse buscar(@PathVariable String id) {
        return mapper.paraResponse(service.buscar(id));
    }

    @PostMapping("/{id}/iniciar-diagnostico")
    public TarefaResponse iniciarDiagnostico(@PathVariable String id) {
        return mapper.paraResponse(service.iniciarDiagnostico(id));
    }

    @PostMapping("/{id}/diagnostico/concluir")
    public TarefaResponse concluirDiagnostico(
            @PathVariable String id, @Valid @RequestBody ConcluirDiagnosticoRequest request) {
        return mapper.paraResponse(service.concluirDiagnostico(id, mapper.paraCommands(request)));
    }

    @PostMapping("/{id}/iniciar-reparo")
    public TarefaResponse iniciarReparo(@PathVariable String id) {
        return mapper.paraResponse(service.iniciarReparoDaFila(id));
    }

    @PostMapping("/{id}/progresso")
    public TarefaResponse registrarProgresso(@PathVariable String id, @Valid @RequestBody ProgressoRequest request) {
        return mapper.paraResponse(service.registrarProgresso(id, request.descricao()));
    }

    @PostMapping("/{id}/finalizar")
    public TarefaResponse finalizar(@PathVariable String id) {
        return mapper.paraResponse(service.finalizarReparo(id));
    }
}

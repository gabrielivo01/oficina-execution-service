package io.github.gabrielivo.oficina.execution.presentation.peca;

import io.github.gabrielivo.oficina.execution.application.peca.PecaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pecas")
public class PecaController {

    private final PecaService service;
    private final PecaMapper mapper;

    public PecaController(PecaService service, PecaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<PecaResponse> criar(@Valid @RequestBody PecaRequest request) {
        PecaResponse response = mapper.paraResponse(service.criar(mapper.paraCriar(request)));
        return ResponseEntity.created(URI.create("/pecas/" + response.id())).body(response);
    }

    @GetMapping
    public List<PecaResponse> listar() {
        return service.listar().stream().map(mapper::paraResponse).toList();
    }

    @GetMapping("/{id}")
    public PecaResponse buscar(@PathVariable String id) {
        return mapper.paraResponse(service.buscar(id));
    }

    @PutMapping("/{id}")
    public PecaResponse atualizar(@PathVariable String id, @Valid @RequestBody PecaRequest request) {
        return mapper.paraResponse(service.atualizar(id, mapper.paraAtualizar(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/repor-estoque")
    public PecaResponse reporEstoque(@PathVariable String id, @RequestParam int quantidade) {
        return mapper.paraResponse(service.reporEstoque(id, quantidade));
    }

    @GetMapping("/alertas/estoque-baixo")
    public List<PecaResponse> estoqueBaixo() {
        return service.listarEstoqueBaixo().stream().map(mapper::paraResponse).toList();
    }

    @GetMapping("/alertas/estoque-critico")
    public List<PecaResponse> estoqueCritico() {
        return service.listarEstoqueCritico().stream().map(mapper::paraResponse).toList();
    }
}

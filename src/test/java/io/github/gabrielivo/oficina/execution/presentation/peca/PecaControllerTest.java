package io.github.gabrielivo.oficina.execution.presentation.peca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.execution.application.peca.PecaService;
import io.github.gabrielivo.oficina.execution.domain.peca.Peca;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PecaControllerTest {

    @Mock
    private PecaService service;

    @Mock
    private PecaMapper mapper;

    private PecaController controller;
    private Peca peca;
    private PecaResponse response;

    @BeforeEach
    void setUp() {
        controller = new PecaController(service, mapper);
        peca = new Peca("Filtro", null, new BigDecimal("10.00"), 4, 1);
        response = new PecaResponse(peca.getId(), peca.getNome(), null, peca.getPreco(), 4, 1, false, false, 0);
    }

    @Test
    void criarRetornaLocationEListaOsResultadosMapeados() {
        PecaRequest request = new PecaRequest("Filtro", null, new BigDecimal("10.00"), 4, 1);
        when(service.criar(org.mockito.ArgumentMatchers.any())).thenReturn(peca);
        when(mapper.paraCriar(request)).thenReturn(null);
        when(mapper.paraResponse(peca)).thenReturn(response);
        when(service.listar()).thenReturn(List.of(peca));

        var created = controller.criar(request);

        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertEquals(
                "/pecas/" + peca.getId(), created.getHeaders().getLocation().getPath());
        assertEquals(List.of(response), controller.listar());
    }

    @Test
    void delegaBuscaAtualizacaoExclusaoReposicaoEAlertas() {
        PecaRequest request = new PecaRequest("Filtro", null, new BigDecimal("10.00"), 4, 1);
        when(service.buscar(peca.getId())).thenReturn(peca);
        when(service.atualizar(org.mockito.ArgumentMatchers.eq(peca.getId()), org.mockito.ArgumentMatchers.any()))
                .thenReturn(peca);
        when(service.reporEstoque(peca.getId(), 2)).thenReturn(peca);
        when(mapper.paraResponse(peca)).thenReturn(response);
        when(service.listarEstoqueBaixo()).thenReturn(List.of(peca));
        when(service.listarEstoqueCritico()).thenReturn(List.of(peca));

        assertEquals(response, controller.buscar(peca.getId()));
        assertEquals(response, controller.atualizar(peca.getId(), request));
        assertEquals(response, controller.reporEstoque(peca.getId(), 2));
        assertEquals(List.of(response), controller.estoqueBaixo());
        assertEquals(List.of(response), controller.estoqueCritico());
        assertEquals(HttpStatus.NO_CONTENT, controller.deletar(peca.getId()).getStatusCode());
        verify(service).deletar(peca.getId());
    }
}

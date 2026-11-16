package io.github.gabrielivo.oficina.execution.presentation.peca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabrielivo.oficina.execution.domain.peca.Peca;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PecaMapperTest {

    private final PecaMapper mapper = new PecaMapper();

    @Test
    void converteRequestDeCriacaoEAtualizacaoParaCommands() {
        PecaRequest request = new PecaRequest("Filtro", "Óleo", new BigDecimal("10.00"), 2, 4);

        var criar = mapper.paraCriar(request);
        var atualizar = mapper.paraAtualizar(request);

        assertEquals(request.nome(), criar.nome());
        assertEquals(request.preco(), criar.preco());
        assertEquals(request.descricao(), atualizar.descricao());
        assertEquals(request.estoqueMinimo(), atualizar.estoqueMinimo());
    }

    @Test
    void respostaExpõeIndicadoresDoEstoque() {
        Peca peca = new Peca("Filtro", null, new BigDecimal("10.00"), 1, 4);

        PecaResponse response = mapper.paraResponse(peca);

        assertEquals(peca.getId(), response.id());
        assertTrue(response.estoqueBaixo());
        assertTrue(response.estoqueCritico());
    }
}

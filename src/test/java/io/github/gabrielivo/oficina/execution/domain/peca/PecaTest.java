package io.github.gabrielivo.oficina.execution.domain.peca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PecaTest {

    @Test
    void deveValidarDadosEControlarEstoque() {
        Peca peca = new Peca("Filtro", null, new BigDecimal("12.50"), 8, 4);

        peca.reduzirEstoque(3);

        assertEquals(5, peca.getQuantidadeEstoque());
        assertFalse(peca.estoqueEstaBaixo());
        peca.reduzirEstoque(1);
        assertTrue(peca.estoqueEstaBaixo());
        assertThrows(IllegalStateException.class, () -> peca.reduzirEstoque(5));
        assertThrows(IllegalArgumentException.class, () -> peca.reporEstoque(0));
    }

    @Test
    void deveRejeitarCadastroInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Peca(" ", null, new BigDecimal("1.00"), 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Peca("Filtro", null, BigDecimal.ZERO, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Peca("Filtro", null, new BigDecimal("1.00"), -1, 0));
    }
}

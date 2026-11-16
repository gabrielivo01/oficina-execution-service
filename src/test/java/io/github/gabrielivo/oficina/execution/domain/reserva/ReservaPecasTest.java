package io.github.gabrielivo.oficina.execution.domain.reserva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.gabrielivo.oficina.execution.domain.peca.Peca;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReservaPecasTest {

    @Test
    void deveReservarEExecutarTransicoesIdempotentes() {
        Peca peca = new Peca("Filtro", null, new BigDecimal("10.00"), 5, 1);
        ReservaPecas reserva = new ReservaPecas("os-1", List.of(peca), List.of(2));

        assertEquals(3, peca.getQuantidadeEstoque());
        assertEquals(StatusReserva.RESERVADA, reserva.getStatus());
        reserva.consumir();
        reserva.consumir();
        assertEquals(StatusReserva.CONSUMIDA, reserva.getStatus());
        assertThrows(IllegalStateException.class, reserva::liberar);
    }

    @Test
    void deveRecusarLiberacaoDeUmaReservaConsumidaSemAlterarEstado() {
        ReservaPecas reserva = new ReservaPecas("os-2", List.of(), List.of());
        reserva.consumir();

        assertThrows(IllegalStateException.class, reserva::liberar);
        assertEquals(StatusReserva.CONSUMIDA, reserva.getStatus());
    }
}

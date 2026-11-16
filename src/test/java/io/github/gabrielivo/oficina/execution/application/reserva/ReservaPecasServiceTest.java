package io.github.gabrielivo.oficina.execution.application.reserva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.execution.domain.peca.Peca;
import io.github.gabrielivo.oficina.execution.domain.peca.PecaRepository;
import io.github.gabrielivo.oficina.execution.domain.reserva.ReservaPecas;
import io.github.gabrielivo.oficina.execution.domain.reserva.ReservaPecasRepository;
import io.github.gabrielivo.oficina.execution.domain.reserva.StatusReserva;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservaPecasServiceTest {

    @Mock
    private PecaRepository pecaRepository;

    @Mock
    private ReservaPecasRepository reservaRepository;

    private ReservaPecasService service;

    @BeforeEach
    void setUp() {
        service = new ReservaPecasService(pecaRepository, reservaRepository);
    }

    @Test
    void deveRetornarReservaExistenteSemReduzirEstoqueNovamente() {
        ReservaPecas existente = new ReservaPecas("os-1", List.of(), List.of());
        when(reservaRepository.findByOsId("os-1")).thenReturn(Optional.of(existente));

        ReservaPecas resultado =
                service.reservar("os-1", List.of(new ReservaPecasService.ItemReservaCommand("p-1", 1)));

        assertSame(existente, resultado);
        verify(pecaRepository, never()).buscarParaReserva(org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    void deveReservarEstoquesAgrupandoItensDaMesmaPeca() {
        Peca peca = new Peca("Filtro", null, new BigDecimal("8.00"), 6, 1);
        when(reservaRepository.findByOsId("os-2")).thenReturn(Optional.empty());
        when(pecaRepository.buscarParaReserva(org.mockito.ArgumentMatchers.argThat(ids -> ids.contains(peca.getId()))))
                .thenReturn(List.of(peca));
        when(reservaRepository.save(org.mockito.ArgumentMatchers.any(ReservaPecas.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservaPecas resultado = service.reservar(
                "os-2",
                List.of(
                        new ReservaPecasService.ItemReservaCommand(peca.getId(), 1),
                        new ReservaPecasService.ItemReservaCommand(peca.getId(), 2)));

        assertEquals(3, peca.getQuantidadeEstoque());
        assertEquals(1, resultado.getItens().size());
        assertEquals(3, resultado.getItens().getFirst().getQuantidade());
    }

    @Test
    void deveRejeitarReservaSemEstoqueDisponivel() {
        Peca peca = new Peca("Filtro", null, new BigDecimal("8.00"), 1, 0);
        when(reservaRepository.findByOsId("os-3")).thenReturn(Optional.empty());
        when(pecaRepository.buscarParaReserva(org.mockito.ArgumentMatchers.argThat(ids -> ids.contains(peca.getId()))))
                .thenReturn(List.of(peca));

        assertThrows(
                IllegalStateException.class,
                () -> service.reservar("os-3", List.of(new ReservaPecasService.ItemReservaCommand(peca.getId(), 2))));
        assertEquals(1, peca.getQuantidadeEstoque());
        verify(reservaRepository, never()).save(org.mockito.ArgumentMatchers.any(ReservaPecas.class));
    }

    @Test
    void cancelarExecucaoReverteConsumoEReponeEstoqueUmaUnicaVez() {
        Peca peca = new Peca("Filtro", null, new BigDecimal("8.00"), 6, 1);
        ReservaPecas reserva = new ReservaPecas("os-4", List.of(peca), List.of(2));
        reserva.consumir();
        when(reservaRepository.findByOsId("os-4")).thenReturn(Optional.of(reserva));
        when(pecaRepository.buscarParaReserva(List.of(peca.getId()))).thenReturn(List.of(peca));

        service.cancelarExecucao("os-4", reserva.getId());
        service.cancelarExecucao("os-4", reserva.getId());

        assertEquals(6, peca.getQuantidadeEstoque());
        assertEquals(StatusReserva.LIBERADA, reserva.getStatus());
    }

    @Test
    void reservaVaziaNaoConsultaCatalogoEValidaItensAntesDeAlterarEstoque() {
        when(reservaRepository.findByOsId("os-5")).thenReturn(Optional.empty());
        when(reservaRepository.save(any(ReservaPecas.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservaPecas vazia = service.reservar("os-5", List.of());

        assertEquals(StatusReserva.RESERVADA, vazia.getStatus());
        assertEquals(0, vazia.getItens().size());
        verify(pecaRepository, never()).buscarParaReserva(anyCollection());
        assertThrows(IllegalArgumentException.class, () -> service.reservar("os-5b", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.reservar("os-5c", List.of(new ReservaPecasService.ItemReservaCommand(" ", 1))));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.reservar("os-5d", List.of(new ReservaPecasService.ItemReservaCommand("peca", 0))));
    }

    @Test
    void rejeitaPecaInexistenteSemGravarReserva() {
        when(reservaRepository.findByOsId("os-6")).thenReturn(Optional.empty());
        when(pecaRepository.buscarParaReserva(anyCollection())).thenReturn(List.of());

        assertThrows(
                io.github.gabrielivo.oficina.execution.application.peca.PecaNaoEncontradaException.class,
                () -> service.reservar("os-6", List.of(new ReservaPecasService.ItemReservaCommand("peca-ausente", 1))));

        verify(reservaRepository, never()).save(any(ReservaPecas.class));
    }

    @Test
    void liberarDevolveEstoqueUmaVezEConsumoEhIdempotente() {
        Peca peca = new Peca("Filtro", null, new BigDecimal("8.00"), 6, 0);
        ReservaPecas reserva = new ReservaPecas("os-7", List.of(peca), List.of(2));
        when(reservaRepository.findByOsId("os-7")).thenReturn(Optional.of(reserva));
        when(pecaRepository.buscarParaReserva(List.of(peca.getId()))).thenReturn(List.of(peca));

        assertSame(reserva, service.liberar("os-7"));
        assertSame(reserva, service.liberar("os-7"));
        assertEquals(StatusReserva.LIBERADA, reserva.getStatus());
        assertEquals(6, peca.getQuantidadeEstoque());
        verify(pecaRepository).buscarParaReserva(List.of(peca.getId()));

        Peca outraPeca = new Peca("Óleo", null, new BigDecimal("8.00"), 6, 0);
        ReservaPecas outraReserva = new ReservaPecas("os-8", List.of(outraPeca), List.of(1));
        when(reservaRepository.findByOsId("os-8")).thenReturn(Optional.of(outraReserva));
        assertSame(outraReserva, service.consumir("os-8"));
        assertSame(outraReserva, service.consumir("os-8"));
        assertEquals(StatusReserva.CONSUMIDA, outraReserva.getStatus());
    }

    @Test
    void rejeitaLiberacaoSemReservaOuComPecaRemovidaDoCatalogo() {
        assertThrows(ReservaPecasService.ReservaNaoEncontradaException.class, () -> service.liberar("os-inexistente"));

        Peca peca = new Peca("Filtro", null, new BigDecimal("8.00"), 6, 0);
        ReservaPecas reserva = new ReservaPecas("os-9", List.of(peca), List.of(1));
        when(reservaRepository.findByOsId("os-9")).thenReturn(Optional.of(reserva));
        when(pecaRepository.buscarParaReserva(List.of(peca.getId()))).thenReturn(List.of());

        assertThrows(
                io.github.gabrielivo.oficina.execution.application.peca.PecaNaoEncontradaException.class,
                () -> service.liberar("os-9"));
    }
}

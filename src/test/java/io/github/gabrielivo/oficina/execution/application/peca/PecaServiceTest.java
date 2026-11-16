package io.github.gabrielivo.oficina.execution.application.peca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.execution.domain.peca.Peca;
import io.github.gabrielivo.oficina.execution.domain.peca.PecaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PecaServiceTest {

    @Mock
    private PecaRepository repository;

    private PecaService service;

    @BeforeEach
    void setUp() {
        service = new PecaService(repository);
    }

    @Test
    void criaBuscaListaAtualizaEDeletaPeca() {
        Peca peca = new Peca("Filtro", "Óleo", new BigDecimal("10.00"), 5, 1);
        when(repository.save(org.mockito.ArgumentMatchers.any(Peca.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findById(peca.getId())).thenReturn(Optional.of(peca));
        when(repository.findAll()).thenReturn(List.of(peca));

        Peca criada = service.criar(new CriarPecaCommand("Filtro", "Óleo", new BigDecimal("10.00"), 5, 1));
        assertEquals("Filtro", criada.getNome());
        assertSame(peca, service.buscar(peca.getId()));
        assertEquals(List.of(peca), service.listar());

        Peca atualizada = service.atualizar(
                peca.getId(), new AtualizarPecaCommand("Filtro novo", null, new BigDecimal("12.00"), 5, 2));
        assertEquals("Filtro novo", atualizada.getNome());
        service.deletar(peca.getId());
        verify(repository).delete(peca);
    }

    @Test
    void repoeEstoqueEConsultaAlertas() {
        Peca peca = new Peca("Filtro", null, new BigDecimal("10.00"), 1, 2);
        when(repository.findById(peca.getId())).thenReturn(Optional.of(peca));
        when(repository.save(peca)).thenReturn(peca);
        when(repository.buscarEstoqueBaixo()).thenReturn(List.of(peca));
        when(repository.buscarEstoqueCritico()).thenReturn(List.of());

        assertEquals(3, service.reporEstoque(peca.getId(), 2).getQuantidadeEstoque());
        assertEquals(List.of(peca), service.listarEstoqueBaixo());
        assertEquals(List.of(), service.listarEstoqueCritico());
    }

    @Test
    void buscaInexistenteExplicitaIdentificadorNaExcecao() {
        when(repository.findById("ausente")).thenReturn(Optional.empty());

        assertEquals(
                "Peça não encontrada: ausente",
                assertThrows(PecaNaoEncontradaException.class, () -> service.buscar("ausente"))
                        .getMessage());
    }
}

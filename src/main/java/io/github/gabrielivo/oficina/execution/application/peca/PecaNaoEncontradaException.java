package io.github.gabrielivo.oficina.execution.application.peca;

public class PecaNaoEncontradaException extends RuntimeException {

    public PecaNaoEncontradaException(String id) {
        super("Peça não encontrada: " + id);
    }
}

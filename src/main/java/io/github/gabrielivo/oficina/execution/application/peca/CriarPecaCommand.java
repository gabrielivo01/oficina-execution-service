package io.github.gabrielivo.oficina.execution.application.peca;

import java.math.BigDecimal;

public record CriarPecaCommand(
        String nome, String descricao, BigDecimal preco, int quantidadeEstoque, int estoqueMinimo) {}

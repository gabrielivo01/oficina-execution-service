package io.github.gabrielivo.oficina.execution.presentation.peca;

import java.math.BigDecimal;

public record PecaResponse(
        String id,
        String nome,
        String descricao,
        BigDecimal preco,
        int quantidadeEstoque,
        int estoqueMinimo,
        boolean estoqueBaixo,
        boolean estoqueCritico,
        long versao) {}

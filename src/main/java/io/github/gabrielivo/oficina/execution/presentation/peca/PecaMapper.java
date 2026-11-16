package io.github.gabrielivo.oficina.execution.presentation.peca;

import io.github.gabrielivo.oficina.execution.application.peca.AtualizarPecaCommand;
import io.github.gabrielivo.oficina.execution.application.peca.CriarPecaCommand;
import io.github.gabrielivo.oficina.execution.domain.peca.Peca;
import org.springframework.stereotype.Component;

@Component
public class PecaMapper {

    public CriarPecaCommand paraCriar(PecaRequest request) {
        return new CriarPecaCommand(
                request.nome(),
                request.descricao(),
                request.preco(),
                request.quantidadeEstoque(),
                request.estoqueMinimo());
    }

    public AtualizarPecaCommand paraAtualizar(PecaRequest request) {
        return new AtualizarPecaCommand(
                request.nome(),
                request.descricao(),
                request.preco(),
                request.quantidadeEstoque(),
                request.estoqueMinimo());
    }

    public PecaResponse paraResponse(Peca peca) {
        return new PecaResponse(
                peca.getId(),
                peca.getNome(),
                peca.getDescricao(),
                peca.getPreco(),
                peca.getQuantidadeEstoque(),
                peca.getEstoqueMinimo(),
                peca.estoqueEstaBaixo(),
                peca.estoqueEstaCritico(),
                peca.getVersao());
    }
}

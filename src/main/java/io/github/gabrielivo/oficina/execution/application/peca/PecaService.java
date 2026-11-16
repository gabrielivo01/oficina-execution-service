package io.github.gabrielivo.oficina.execution.application.peca;

import io.github.gabrielivo.oficina.execution.domain.peca.Peca;
import io.github.gabrielivo.oficina.execution.domain.peca.PecaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PecaService {

    private final PecaRepository repository;

    public PecaService(PecaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Peca criar(CriarPecaCommand command) {
        return repository.save(new Peca(
                command.nome(),
                command.descricao(),
                command.preco(),
                command.quantidadeEstoque(),
                command.estoqueMinimo()));
    }

    @Transactional(readOnly = true)
    public Peca buscar(String id) {
        return repository.findById(id).orElseThrow(() -> new PecaNaoEncontradaException(id));
    }

    @Transactional(readOnly = true)
    public List<Peca> listar() {
        return repository.findAll();
    }

    @Transactional
    public Peca atualizar(String id, AtualizarPecaCommand command) {
        Peca peca = buscar(id);
        peca.atualizar(
                command.nome(),
                command.descricao(),
                command.preco(),
                command.quantidadeEstoque(),
                command.estoqueMinimo());
        return repository.save(peca);
    }

    @Transactional
    public void deletar(String id) {
        repository.delete(buscar(id));
    }

    @Transactional
    public Peca reporEstoque(String id, int quantidade) {
        Peca peca = buscar(id);
        peca.reporEstoque(quantidade);
        return repository.save(peca);
    }

    @Transactional(readOnly = true)
    public List<Peca> listarEstoqueBaixo() {
        return repository.buscarEstoqueBaixo();
    }

    @Transactional(readOnly = true)
    public List<Peca> listarEstoqueCritico() {
        return repository.buscarEstoqueCritico();
    }
}

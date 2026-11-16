package io.github.gabrielivo.oficina.execution.domain.execucao;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TarefaExecucaoRepository extends JpaRepository<TarefaExecucao, String> {

    boolean existsByOsIdAndTipo(String osId, TipoTarefa tipo);

    Optional<TarefaExecucao> findByOsIdAndTipo(String osId, TipoTarefa tipo);

    @EntityGraph(attributePaths = "itens")
    @Query("select t from TarefaExecucao t where t.id = :id")
    Optional<TarefaExecucao> buscarComItens(@Param("id") String id);

    @EntityGraph(attributePaths = "itens")
    List<TarefaExecucao> findByStatus(StatusTarefa status, Sort sort);
}

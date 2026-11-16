package io.github.gabrielivo.oficina.execution.domain.peca;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PecaRepository extends JpaRepository<Peca, String> {

    @Query("select p from Peca p where p.quantidadeEstoque <= p.estoqueMinimo order by p.nome")
    List<Peca> buscarEstoqueBaixo();

    @Query("select p from Peca p where p.quantidadeEstoque * 2 <= p.estoqueMinimo order by p.nome")
    List<Peca> buscarEstoqueCritico();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Peca p where p.id in :ids order by p.nome")
    List<Peca> buscarParaReserva(@Param("ids") Collection<String> ids);
}

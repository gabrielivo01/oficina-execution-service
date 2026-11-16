package io.github.gabrielivo.oficina.execution.domain.mensageria;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensagemOutboxRepository extends JpaRepository<MensagemOutbox, String> {
    List<MensagemOutbox> findByEnviadoEmIsNullOrderByCriadoEmAsc(Pageable pageable);
}

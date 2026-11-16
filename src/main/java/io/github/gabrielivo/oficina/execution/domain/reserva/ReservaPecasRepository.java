package io.github.gabrielivo.oficina.execution.domain.reserva;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaPecasRepository extends JpaRepository<ReservaPecas, String> {
    Optional<ReservaPecas> findByOsId(String osId);
}

package io.github.gabrielivo.oficina.execution.application.reserva;

import io.github.gabrielivo.oficina.execution.application.peca.PecaNaoEncontradaException;
import io.github.gabrielivo.oficina.execution.domain.peca.Peca;
import io.github.gabrielivo.oficina.execution.domain.peca.PecaRepository;
import io.github.gabrielivo.oficina.execution.domain.reserva.ReservaPecas;
import io.github.gabrielivo.oficina.execution.domain.reserva.ReservaPecasRepository;
import io.github.gabrielivo.oficina.execution.domain.reserva.StatusReserva;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaPecasService {

    private final PecaRepository pecaRepository;
    private final ReservaPecasRepository reservaRepository;

    public ReservaPecasService(PecaRepository pecaRepository, ReservaPecasRepository reservaRepository) {
        this.pecaRepository = pecaRepository;
        this.reservaRepository = reservaRepository;
    }

    @Transactional
    public ReservaPecas reservar(String osId, List<ItemReservaCommand> itens) {
        var existente = reservaRepository.findByOsId(osId);
        if (existente.isPresent()) {
            return existente.get();
        }
        if (itens == null) {
            throw new IllegalArgumentException("Os itens da reserva são obrigatórios.");
        }
        Map<String, Integer> quantidades = new LinkedHashMap<>();
        for (ItemReservaCommand item : itens) {
            if (item.pecaId() == null || item.pecaId().isBlank() || item.quantidade() <= 0) {
                throw new IllegalArgumentException("Cada item de reserva exige pecaId e quantidade positiva.");
            }
            quantidades.merge(item.pecaId(), item.quantidade(), Math::addExact);
        }
        List<Peca> pecas = quantidades.isEmpty() ? List.of() : pecaRepository.buscarParaReserva(quantidades.keySet());
        Map<String, Peca> porId = new LinkedHashMap<>();
        pecas.forEach(peca -> porId.put(peca.getId(), peca));
        if (porId.size() != quantidades.size()) {
            String ausente = quantidades.keySet().stream()
                    .filter(id -> !porId.containsKey(id))
                    .findFirst()
                    .orElseThrow();
            throw new PecaNaoEncontradaException(ausente);
        }
        List<Peca> pecasReserva = new ArrayList<>();
        List<Integer> quantidadesReserva = new ArrayList<>();
        for (var entry : quantidades.entrySet()) {
            pecasReserva.add(porId.get(entry.getKey()));
            quantidadesReserva.add(entry.getValue());
        }
        return reservaRepository.save(new ReservaPecas(osId, pecasReserva, quantidadesReserva));
    }

    @Transactional
    public ReservaPecas liberar(String osId) {
        ReservaPecas reserva = buscar(osId);
        if (reserva.getStatus() == StatusReserva.LIBERADA) {
            return reserva;
        }
        if (reserva.getStatus() == StatusReserva.CONSUMIDA) {
            throw new IllegalStateException("Uma reserva consumida não pode ser liberada.");
        }
        List<Peca> pecas = reserva.getItens().isEmpty()
                ? List.of()
                : pecaRepository.buscarParaReserva(reserva.getItens().stream()
                        .map(item -> item.getPecaId())
                        .toList());
        Map<String, Peca> porId = new LinkedHashMap<>();
        pecas.forEach(peca -> porId.put(peca.getId(), peca));
        for (var item : reserva.getItens()) {
            Peca peca = porId.get(item.getPecaId());
            if (peca == null) {
                throw new PecaNaoEncontradaException(item.getPecaId());
            }
            peca.reporEstoque(item.getQuantidade());
        }
        reserva.liberar();
        return reserva;
    }

    @Transactional
    public ReservaPecas consumir(String osId) {
        ReservaPecas reserva = buscar(osId);
        reserva.consumir();
        return reserva;
    }

    @Transactional
    public ReservaPecas cancelarExecucao(String osId, String reservaIdSolicitada) {
        ReservaPecas reserva = buscar(osId);
        if (reservaIdSolicitada == null || !reserva.getId().equals(reservaIdSolicitada)) {
            throw new IllegalArgumentException("A reserva informada não corresponde à reserva da OS.");
        }
        if (reserva.getStatus() == StatusReserva.LIBERADA) {
            return reserva;
        }
        if (reserva.getStatus() == StatusReserva.RESERVADA) {
            return liberar(osId);
        }
        reporPecas(reserva);
        reserva.reverterConsumo();
        return reserva;
    }

    private void reporPecas(ReservaPecas reserva) {
        if (reserva.getItens().isEmpty()) {
            return;
        }
        List<Peca> pecas = pecaRepository.buscarParaReserva(
                reserva.getItens().stream().map(item -> item.getPecaId()).toList());
        Map<String, Peca> porId = new LinkedHashMap<>();
        pecas.forEach(peca -> porId.put(peca.getId(), peca));
        for (var item : reserva.getItens()) {
            Peca peca = porId.get(item.getPecaId());
            if (peca == null) {
                throw new PecaNaoEncontradaException(item.getPecaId());
            }
            peca.reporEstoque(item.getQuantidade());
        }
    }

    @Transactional(readOnly = true)
    public ReservaPecas buscar(String osId) {
        return reservaRepository.findByOsId(osId).orElseThrow(() -> new ReservaNaoEncontradaException(osId));
    }

    public record ItemReservaCommand(String pecaId, int quantidade) {}

    public static class ReservaNaoEncontradaException extends RuntimeException {
        public ReservaNaoEncontradaException(String osId) {
            super("Reserva de peças não encontrada para a OS: " + osId);
        }
    }
}

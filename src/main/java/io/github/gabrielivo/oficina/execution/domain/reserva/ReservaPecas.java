package io.github.gabrielivo.oficina.execution.domain.reserva;

import io.github.gabrielivo.oficina.execution.domain.peca.Peca;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reserva_pecas")
public class ReservaPecas {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "os_id", nullable = false, unique = true, length = 36)
    private String osId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusReserva status;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("nomePeca ASC")
    private List<ItemReservaPecas> itens = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    protected ReservaPecas() {}

    public ReservaPecas(String osId, List<Peca> pecas, List<Integer> quantidades) {
        if (osId == null || osId.isBlank()) {
            throw new IllegalArgumentException("osId é obrigatório.");
        }
        if (pecas == null || quantidades == null || pecas.size() != quantidades.size()) {
            throw new IllegalArgumentException("Peças e quantidades devem corresponder.");
        }
        this.id = UUID.randomUUID().toString();
        this.osId = osId;
        this.status = StatusReserva.RESERVADA;
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = this.criadoEm;
        for (int i = 0; i < pecas.size(); i++) {
            pecas.get(i).validarEstoqueParaReducao(quantidades.get(i));
        }
        for (int i = 0; i < pecas.size(); i++) {
            Peca peca = pecas.get(i);
            int quantidade = quantidades.get(i);
            peca.reduzirEstoque(quantidade);
            itens.add(new ItemReservaPecas(this, peca.getId(), peca.getNome(), quantidade, peca.getPreco()));
        }
    }

    public void liberar() {
        if (status == StatusReserva.LIBERADA) {
            return;
        }
        if (status == StatusReserva.CONSUMIDA) {
            throw new IllegalStateException("Uma reserva consumida não pode ser liberada.");
        }
        status = StatusReserva.LIBERADA;
        atualizadoEm = LocalDateTime.now();
    }

    public void consumir() {
        if (status == StatusReserva.CONSUMIDA) {
            return;
        }
        if (status != StatusReserva.RESERVADA) {
            throw new IllegalStateException("Somente uma reserva ativa pode ser consumida.");
        }
        status = StatusReserva.CONSUMIDA;
        atualizadoEm = LocalDateTime.now();
    }

    public void reverterConsumo() {
        if (status == StatusReserva.LIBERADA) {
            return;
        }
        if (status != StatusReserva.CONSUMIDA) {
            throw new IllegalStateException("Somente uma reserva consumida pode ter o consumo revertido.");
        }
        status = StatusReserva.LIBERADA;
        atualizadoEm = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getOsId() {
        return osId;
    }

    public StatusReserva getStatus() {
        return status;
    }

    public List<ItemReservaPecas> getItens() {
        return Collections.unmodifiableList(itens);
    }
}

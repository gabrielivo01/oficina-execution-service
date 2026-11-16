package io.github.gabrielivo.oficina.execution.domain.reserva;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "item_reserva_pecas")
public class ItemReservaPecas {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private ReservaPecas reserva;

    @Column(name = "peca_id", nullable = false, length = 36)
    private String pecaId;

    @Column(name = "nome_peca", nullable = false, length = 150)
    private String nomePeca;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    protected ItemReservaPecas() {}

    ItemReservaPecas(ReservaPecas reserva, String pecaId, String nomePeca, int quantidade, BigDecimal precoUnitario) {
        this.id = UUID.randomUUID().toString();
        this.reserva = reserva;
        this.pecaId = pecaId;
        this.nomePeca = nomePeca;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    public String getPecaId() {
        return pecaId;
    }

    public String getNomePeca() {
        return nomePeca;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }
}

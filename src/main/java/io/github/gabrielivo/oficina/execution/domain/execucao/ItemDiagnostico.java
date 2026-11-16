package io.github.gabrielivo.oficina.execution.domain.execucao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "item_diagnostico")
public class ItemDiagnostico {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tarefa_id", nullable = false)
    private TarefaExecucao tarefa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoItemDiagnostico tipo;

    @Column(nullable = false, length = 150)
    private String descricao;

    @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "peca_id", length = 36)
    private String pecaId;

    @Column(nullable = false)
    private int quantidade;

    protected ItemDiagnostico() {}

    public ItemDiagnostico(
            TipoItemDiagnostico tipo, String descricao, BigDecimal valorUnitario, String pecaId, int quantidade) {
        if (tipo == null || descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Tipo e descrição do item são obrigatórios.");
        }
        if (valorUnitario == null || valorUnitario.signum() < 0 || quantidade <= 0) {
            throw new IllegalArgumentException("Valor deve ser não negativo e quantidade deve ser positiva.");
        }
        if (tipo == TipoItemDiagnostico.PECA && (pecaId == null || pecaId.isBlank())) {
            throw new IllegalArgumentException("pecaId é obrigatório para itens do tipo PECA.");
        }
        if (tipo == TipoItemDiagnostico.SERVICO && pecaId != null) {
            throw new IllegalArgumentException("Serviços não podem referenciar uma peça.");
        }
        if (tipo == TipoItemDiagnostico.SERVICO && quantidade != 1) {
            throw new IllegalArgumentException("A quantidade de um serviço deve ser 1.");
        }
        this.id = UUID.randomUUID().toString();
        this.tipo = tipo;
        this.descricao = descricao.strip();
        this.valorUnitario = valorUnitario;
        this.pecaId = pecaId;
        this.quantidade = quantidade;
    }

    void associar(TarefaExecucao tarefa) {
        this.tarefa = tarefa;
    }

    public TipoItemDiagnostico getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public String getPecaId() {
        return pecaId;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getValorTotal() {
        return valorUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}

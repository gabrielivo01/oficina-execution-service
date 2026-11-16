package io.github.gabrielivo.oficina.execution.domain.peca;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "peca")
public class Peca {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "quantidade_estoque", nullable = false)
    private int quantidadeEstoque;

    @Column(name = "estoque_minimo", nullable = false)
    private int estoqueMinimo;

    @Version
    @Column(nullable = false)
    private long versao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    protected Peca() {}

    public Peca(String nome, String descricao, BigDecimal preco, int quantidadeEstoque, int estoqueMinimo) {
        validar(nome, preco, quantidadeEstoque, estoqueMinimo);
        this.id = UUID.randomUUID().toString();
        this.nome = nome.strip();
        this.descricao = descricao;
        this.preco = preco;
        this.quantidadeEstoque = quantidadeEstoque;
        this.estoqueMinimo = estoqueMinimo;
    }

    public void atualizar(String nome, String descricao, BigDecimal preco, int quantidadeEstoque, int estoqueMinimo) {
        validar(nome, preco, quantidadeEstoque, estoqueMinimo);
        this.nome = nome.strip();
        this.descricao = descricao;
        this.preco = preco;
        this.quantidadeEstoque = quantidadeEstoque;
        this.estoqueMinimo = estoqueMinimo;
    }

    public void reduzirEstoque(int quantidade) {
        validarEstoqueParaReducao(quantidade);
        quantidadeEstoque -= quantidade;
    }

    public void validarEstoqueParaReducao(int quantidade) {
        validarQuantidade(quantidade);
        if (quantidadeEstoque < quantidade) {
            throw new IllegalStateException(
                    "Estoque insuficiente para '" + nome + "'. Disponível: " + quantidadeEstoque);
        }
    }

    public void reporEstoque(int quantidade) {
        validarQuantidade(quantidade);
        quantidadeEstoque = Math.addExact(quantidadeEstoque, quantidade);
    }

    public boolean estoqueEstaBaixo() {
        return quantidadeEstoque <= estoqueMinimo;
    }

    public boolean estoqueEstaCritico() {
        return quantidadeEstoque <= estoqueMinimo / 2;
    }

    private static void validar(String nome, BigDecimal preco, int estoque, int minimo) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da peça é obrigatório.");
        }
        if (nome.strip().length() > 150) {
            throw new IllegalArgumentException("Nome da peça deve ter no máximo 150 caracteres.");
        }
        if (preco == null || preco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço deve ser maior que zero.");
        }
        if (estoque < 0 || minimo < 0) {
            throw new IllegalArgumentException("Estoque e estoque mínimo não podem ser negativos.");
        }
    }

    private static void validarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }
    }

    @PrePersist
    void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public long getVersao() {
        return versao;
    }
}

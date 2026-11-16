package io.github.gabrielivo.oficina.execution.domain.execucao;

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
@Table(name = "tarefa_execucao")
public class TarefaExecucao {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "os_id", nullable = false, length = 36)
    private String osId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTarefa tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTarefa status;

    @Column(length = 10)
    private String placa;

    @Column(length = 1000)
    private String observacao;

    @Column(nullable = false)
    private int prioridade;

    @Column(length = 2000)
    private String progresso;

    @OneToMany(mappedBy = "tarefa", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("descricao ASC")
    private List<ItemDiagnostico> itens = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "iniciado_em")
    private LocalDateTime iniciadoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    protected TarefaExecucao() {}

    public TarefaExecucao(String osId, TipoTarefa tipo, String placa, String observacao, int prioridade) {
        if (osId == null || osId.isBlank() || tipo == null) {
            throw new IllegalArgumentException("osId e tipo da tarefa são obrigatórios.");
        }
        this.id = UUID.randomUUID().toString();
        this.osId = osId;
        this.tipo = tipo;
        this.status = StatusTarefa.NA_FILA;
        this.placa = placa;
        this.observacao = observacao;
        this.prioridade = prioridade;
        this.criadoEm = LocalDateTime.now();
    }

    public void iniciar() {
        exigirStatus(StatusTarefa.NA_FILA);
        status = StatusTarefa.EM_ANDAMENTO;
        iniciadoEm = LocalDateTime.now();
    }

    public void concluirDiagnostico(List<ItemDiagnostico> itens) {
        if (tipo != TipoTarefa.DIAGNOSTICO) {
            throw new IllegalStateException("Somente uma tarefa de diagnóstico pode receber itens.");
        }
        exigirStatus(StatusTarefa.EM_ANDAMENTO);
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("O diagnóstico deve conter pelo menos um item.");
        }
        this.itens.clear();
        for (ItemDiagnostico item : itens) {
            item.associar(this);
            this.itens.add(item);
        }
        concluir();
    }

    public void registrarProgresso(String descricao) {
        exigirStatus(StatusTarefa.EM_ANDAMENTO);
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Descrição do progresso é obrigatória.");
        }
        String linha = LocalDateTime.now() + " - " + descricao.strip();
        progresso = progresso == null || progresso.isBlank() ? linha : progresso + "\n" + linha;
    }

    public void finalizarReparo() {
        if (tipo != TipoTarefa.REPARO) {
            throw new IllegalStateException("Somente uma tarefa de reparo pode ser finalizada.");
        }
        exigirStatus(StatusTarefa.EM_ANDAMENTO);
        concluir();
    }

    public void recusar() {
        exigirStatus(StatusTarefa.NA_FILA);
        status = StatusTarefa.RECUSADA;
        concluidoEm = LocalDateTime.now();
    }

    public void cancelar() {
        if (tipo != TipoTarefa.REPARO) {
            throw new IllegalStateException("Somente uma tarefa de reparo pode ser cancelada.");
        }
        if (status != StatusTarefa.NA_FILA && status != StatusTarefa.EM_ANDAMENTO) {
            if (status == StatusTarefa.CANCELADA) {
                return;
            }
            throw new IllegalStateException("Somente uma tarefa de reparo não finalizada pode ser cancelada.");
        }
        status = StatusTarefa.CANCELADA;
        concluidoEm = LocalDateTime.now();
    }

    private void concluir() {
        status = StatusTarefa.CONCLUIDA;
        concluidoEm = LocalDateTime.now();
    }

    private void exigirStatus(StatusTarefa esperado) {
        if (status != esperado) {
            throw new IllegalStateException("A tarefa deve estar " + esperado + " para esta operação.");
        }
    }

    public String getId() {
        return id;
    }

    public String getOsId() {
        return osId;
    }

    public TipoTarefa getTipo() {
        return tipo;
    }

    public StatusTarefa getStatus() {
        return status;
    }

    public String getPlaca() {
        return placa;
    }

    public String getObservacao() {
        return observacao;
    }

    public int getPrioridade() {
        return prioridade;
    }

    public String getProgresso() {
        return progresso;
    }

    public List<ItemDiagnostico> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getIniciadoEm() {
        return iniciadoEm;
    }

    public LocalDateTime getConcluidoEm() {
        return concluidoEm;
    }
}

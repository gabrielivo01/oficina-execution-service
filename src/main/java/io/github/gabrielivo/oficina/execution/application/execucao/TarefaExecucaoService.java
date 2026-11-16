package io.github.gabrielivo.oficina.execution.application.execucao;

import io.github.gabrielivo.oficina.execution.application.mensageria.OutboxService;
import io.github.gabrielivo.oficina.execution.application.reserva.ReservaPecasService;
import io.github.gabrielivo.oficina.execution.application.reserva.ReservaPecasService.ItemReservaCommand;
import io.github.gabrielivo.oficina.execution.domain.execucao.ItemDiagnostico;
import io.github.gabrielivo.oficina.execution.domain.execucao.StatusTarefa;
import io.github.gabrielivo.oficina.execution.domain.execucao.TarefaExecucao;
import io.github.gabrielivo.oficina.execution.domain.execucao.TarefaExecucaoRepository;
import io.github.gabrielivo.oficina.execution.domain.execucao.TipoItemDiagnostico;
import io.github.gabrielivo.oficina.execution.domain.execucao.TipoTarefa;
import io.github.gabrielivo.oficina.execution.domain.peca.Peca;
import io.github.gabrielivo.oficina.execution.domain.peca.PecaRepository;
import io.github.gabrielivo.oficina.execution.domain.reserva.ReservaPecas;
import io.github.gabrielivo.oficina.execution.domain.reserva.ReservaPecasRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TarefaExecucaoService {

    private final TarefaExecucaoRepository tarefaRepository;
    private final PecaRepository pecaRepository;
    private final ReservaPecasRepository reservaRepository;
    private final ReservaPecasService reservaService;
    private final OutboxService outboxService;
    private final SimulacaoProperties simulacao;

    public TarefaExecucaoService(
            TarefaExecucaoRepository tarefaRepository,
            PecaRepository pecaRepository,
            ReservaPecasRepository reservaRepository,
            ReservaPecasService reservaService,
            OutboxService outboxService,
            SimulacaoProperties simulacao) {
        this.tarefaRepository = tarefaRepository;
        this.pecaRepository = pecaRepository;
        this.reservaRepository = reservaRepository;
        this.reservaService = reservaService;
        this.outboxService = outboxService;
        this.simulacao = simulacao;
    }

    @Transactional
    public TarefaExecucao solicitarDiagnostico(SolicitarDiagnosticoCommand command) {
        return tarefaRepository
                .findByOsIdAndTipo(command.osId(), TipoTarefa.DIAGNOSTICO)
                .orElseGet(() -> tarefaRepository.save(new TarefaExecucao(
                        command.osId(),
                        TipoTarefa.DIAGNOSTICO,
                        command.placa(),
                        command.observacao(),
                        command.prioridade())));
    }

    @Transactional(readOnly = true)
    public List<TarefaExecucao> listar(StatusTarefa status) {
        return tarefaRepository.findByStatus(
                status, Sort.by(Sort.Order.desc("prioridade"), Sort.Order.asc("criadoEm")));
    }

    @Transactional(readOnly = true)
    public TarefaExecucao buscar(String id) {
        return tarefaRepository.buscarComItens(id).orElseThrow(() -> new TarefaNaoEncontradaException(id));
    }

    @Transactional
    public TarefaExecucao iniciarDiagnostico(String id) {
        TarefaExecucao tarefa = buscar(id);
        exigirTipo(tarefa, TipoTarefa.DIAGNOSTICO);
        tarefa.iniciar();
        outboxService.registrar(
                "DiagnosticoIniciado",
                tarefa.getOsId(),
                Map.of(
                        "tarefaId",
                        tarefa.getId(),
                        "iniciadoEm",
                        tarefa.getIniciadoEm().toString()));
        return tarefa;
    }

    @Transactional
    public TarefaExecucao concluirDiagnostico(String id, List<ItemDiagnosticoCommand> comandos) {
        TarefaExecucao tarefa = buscar(id);
        exigirTipo(tarefa, TipoTarefa.DIAGNOSTICO);
        if (comandos == null || comandos.isEmpty()) {
            throw new IllegalArgumentException("O diagnóstico deve conter pelo menos um item.");
        }
        List<ItemDiagnostico> itens = comandos.stream().map(this::criarSnapshot).toList();
        List<ItemReservaCommand> pecas = itens.stream()
                .filter(item -> item.getTipo() == TipoItemDiagnostico.PECA)
                .map(item -> new ItemReservaCommand(item.getPecaId(), item.getQuantidade()))
                .toList();
        ReservaPecas reserva = reservaService.reservar(tarefa.getOsId(), pecas);
        tarefa.concluirDiagnostico(itens);
        List<Map<String, Object>> snapshot =
                itens.stream().map(this::paraPayload).toList();
        BigDecimal valorTotal =
                itens.stream().map(ItemDiagnostico::getValorTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        outboxService.registrar(
                "DiagnosticoConcluido",
                tarefa.getOsId(),
                Map.of(
                        "tarefaId",
                        tarefa.getId(),
                        "reservaId",
                        reserva.getId(),
                        "itens",
                        snapshot,
                        "valorTotal",
                        valorTotal,
                        "concluidoEm",
                        tarefa.getConcluidoEm().toString()));
        return tarefa;
    }

    @Transactional
    public void iniciarReparo(String osId, String reservaId, String cobrancaId, String placa) {
        if (cobrancaId == null || cobrancaId.isBlank()) {
            throw new IllegalArgumentException("cobrancaId é obrigatório para iniciar a execução.");
        }
        ReservaPecas reserva = reservaService.buscar(osId);
        if (reservaId == null || !reserva.getId().equals(reservaId)) {
            throw new IllegalArgumentException("A reserva informada não corresponde à reserva da OS.");
        }
        var tarefaExistente = tarefaRepository.findByOsIdAndTipo(osId, TipoTarefa.REPARO);
        if (tarefaExistente.isPresent() && tarefaExistente.get().getStatus() != StatusTarefa.NA_FILA) {
            return;
        }
        if (simulacao.recusarInicioExecucao()) {
            TarefaExecucao tarefaRecusada = tarefaExistente.orElseGet(
                    () -> tarefaRepository.save(new TarefaExecucao(osId, TipoTarefa.REPARO, placa, null, 0)));
            tarefaRecusada.recusar();
            outboxService.registrar(
                    "ExecucaoRecusada",
                    osId,
                    Map.of(
                            "reservaId",
                            reserva.getId(),
                            "motivo",
                            "Execução recusada pela simulação de falha (app.simulacao.recusar-inicio-execucao)",
                            "recusadaEm",
                            Instant.now().toString()));
            return;
        }
        reservaService.consumir(osId);
        TarefaExecucao tarefa = tarefaExistente.orElseGet(
                () -> tarefaRepository.save(new TarefaExecucao(osId, TipoTarefa.REPARO, placa, null, 0)));
        tarefa.iniciar();
        outboxService.registrar(
                "ExecucaoIniciada",
                osId,
                Map.of(
                        "tarefaId",
                        tarefa.getId(),
                        "reservaId",
                        reserva.getId(),
                        "iniciadaEm",
                        tarefa.getIniciadoEm().toString()));
    }

    @Transactional
    public TarefaExecucao registrarProgresso(String id, String descricao) {
        TarefaExecucao tarefa = buscar(id);
        exigirTipo(tarefa, TipoTarefa.REPARO);
        tarefa.registrarProgresso(descricao);
        return tarefa;
    }

    @Transactional
    public TarefaExecucao iniciarReparoDaFila(String id) {
        TarefaExecucao tarefa = buscar(id);
        exigirTipo(tarefa, TipoTarefa.REPARO);
        if (tarefa.getStatus() == StatusTarefa.EM_ANDAMENTO) {
            return tarefa;
        }
        ReservaPecas reserva = reservaService.consumir(tarefa.getOsId());
        tarefa.iniciar();
        outboxService.registrar(
                "ExecucaoIniciada",
                tarefa.getOsId(),
                Map.of(
                        "tarefaId",
                        tarefa.getId(),
                        "reservaId",
                        reserva.getId(),
                        "iniciadaEm",
                        tarefa.getIniciadoEm().toString()));
        return tarefa;
    }

    @Transactional
    public TarefaExecucao finalizarReparo(String id) {
        TarefaExecucao tarefa = buscar(id);
        exigirTipo(tarefa, TipoTarefa.REPARO);
        tarefa.finalizarReparo();
        outboxService.registrar(
                "ExecucaoFinalizada",
                tarefa.getOsId(),
                Map.of(
                        "tarefaId",
                        tarefa.getId(),
                        "finalizadaEm",
                        tarefa.getConcluidoEm().toString()));
        return tarefa;
    }

    @Transactional
    public void cancelarReparo(String osId, String reservaId) {
        if (osId == null || osId.isBlank() || reservaId == null || reservaId.isBlank()) {
            throw new IllegalArgumentException("osId e reservaId são obrigatórios para cancelar a execução.");
        }
        TarefaExecucao tarefa = tarefaRepository
                .findByOsIdAndTipo(osId, TipoTarefa.REPARO)
                .orElseGet(() -> tarefaRepository.save(new TarefaExecucao(osId, TipoTarefa.REPARO, null, null, 0)));
        if (tarefa.getStatus() == StatusTarefa.CONCLUIDA) {
            outboxService.registrar(
                    "ExecucaoFinalizada",
                    osId,
                    Map.of(
                            "tarefaId",
                            tarefa.getId(),
                            "finalizadaEm",
                            tarefa.getConcluidoEm().toString()));
            return;
        }
        if (tarefa.getStatus() == StatusTarefa.RECUSADA) {
            outboxService.registrar(
                    "ExecucaoRecusada",
                    osId,
                    Map.of(
                            "reservaId",
                            reservaId,
                            "motivo",
                            "Execução recusada antes da confirmação do cancelamento."));
            return;
        }
        tarefa.cancelar();
        reservaService.cancelarExecucao(osId, reservaId);
        outboxService.registrar(
                "ExecucaoCancelada",
                osId,
                Map.of(
                        "tarefaId",
                        tarefa.getId(),
                        "reservaId",
                        reservaId,
                        "canceladaEm",
                        Instant.now().toString()));
    }

    @Transactional
    public void liberarReserva(String osId, String reservaIdSolicitada) {
        var reservaExistente = reservaRepository.findByOsId(osId);
        if (reservaExistente.isEmpty()) {
            String id = reservaIdSolicitada == null
                    ? UUID.nameUUIDFromBytes(("reserva-vazia:" + osId).getBytes(StandardCharsets.UTF_8))
                            .toString()
                    : reservaIdSolicitada;
            publicarReservaLiberada(osId, id, List.of());
            return;
        }
        ReservaPecas reserva = reservaExistente.get();
        if (reservaIdSolicitada != null && !reserva.getId().equals(reservaIdSolicitada)) {
            throw new IllegalArgumentException("A reserva informada não corresponde à reserva da OS.");
        }
        if (reserva.getStatus() == io.github.gabrielivo.oficina.execution.domain.reserva.StatusReserva.CONSUMIDA) {
            throw new IllegalStateException("Uma reserva consumida não pode ser liberada.");
        }
        List<Map<String, Object>> liberados = new ArrayList<>();
        if (reserva.getStatus() == io.github.gabrielivo.oficina.execution.domain.reserva.StatusReserva.RESERVADA) {
            reservaService.liberar(osId);
            liberados = reserva.getItens().stream()
                    .map(item -> Map.<String, Object>of("pecaId", item.getPecaId(), "quantidade", item.getQuantidade()))
                    .toList();
        }
        publicarReservaLiberada(osId, reserva.getId(), liberados);
    }

    private ItemDiagnostico criarSnapshot(ItemDiagnosticoCommand command) {
        if (command.tipo() == TipoItemDiagnostico.SERVICO) {
            return new ItemDiagnostico(
                    command.tipo(), command.descricao(), command.valorUnitario(), null, command.quantidade());
        }
        Peca peca = pecaRepository
                .findById(command.pecaId())
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada: " + command.pecaId()));
        return new ItemDiagnostico(
                TipoItemDiagnostico.PECA, peca.getNome(), peca.getPreco(), peca.getId(), command.quantidade());
    }

    private Map<String, Object> paraPayload(ItemDiagnostico item) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tipo", item.getTipo().name());
        payload.put("descricao", item.getDescricao());
        payload.put("valorUnitario", item.getValorUnitario());
        payload.put("pecaId", item.getPecaId());
        payload.put("quantidade", item.getQuantidade());
        payload.put("valorTotal", item.getValorTotal());
        return payload;
    }

    private void publicarReservaLiberada(String osId, String reservaId, List<Map<String, Object>> itens) {
        outboxService.registrar(
                "ReservaPecasLiberada",
                osId,
                Map.of(
                        "reservaId",
                        reservaId,
                        "itensLiberados",
                        itens,
                        "liberadaEm",
                        Instant.now().toString()));
    }

    private void exigirTipo(TarefaExecucao tarefa, TipoTarefa tipo) {
        if (tarefa.getTipo() != tipo) {
            throw new IllegalStateException("A tarefa não é do tipo " + tipo + ".");
        }
    }

    public static class TarefaNaoEncontradaException extends RuntimeException {
        public TarefaNaoEncontradaException(String id) {
            super("Tarefa de execução não encontrada: " + id);
        }
    }
}

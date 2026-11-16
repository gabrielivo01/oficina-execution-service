package io.github.gabrielivo.oficina.execution.application.execucao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.execution.application.mensageria.OutboxService;
import io.github.gabrielivo.oficina.execution.application.reserva.ReservaPecasService;
import io.github.gabrielivo.oficina.execution.application.reserva.ReservaPecasService.ItemReservaCommand;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TarefaExecucaoServiceTest {

    @Mock
    private TarefaExecucaoRepository tarefaRepository;

    @Mock
    private PecaRepository pecaRepository;

    @Mock
    private ReservaPecasRepository reservaRepository;

    @Mock
    private ReservaPecasService reservaService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private SimulacaoProperties simulacao;

    private TarefaExecucaoService service;

    @BeforeEach
    void setUp() {
        service = new TarefaExecucaoService(
                tarefaRepository, pecaRepository, reservaRepository, reservaService, outboxService, simulacao);
    }

    @Test
    void deveConcluirDiagnosticoReservandoPecasEPublicandoSnapshot() {
        TarefaExecucao tarefa = tarefaEmAndamento(TipoTarefa.DIAGNOSTICO, "os-1");
        ReservaPecas reserva = new ReservaPecas("os-1", List.of(), List.of());
        when(tarefaRepository.buscarComItens(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(reservaService.reservar(eq("os-1"), anyList())).thenReturn(reserva);
        var comando = new ItemDiagnosticoCommand(
                TipoItemDiagnostico.SERVICO, "Diagnóstico", new BigDecimal("40.00"), null, 1);

        TarefaExecucao resultado = service.concluirDiagnostico(tarefa.getId(), List.of(comando));

        assertEquals(StatusTarefa.CONCLUIDA, resultado.getStatus());
        assertEquals("Diagnóstico", resultado.getItens().getFirst().getDescricao());
        verify(reservaService).reservar(eq("os-1"), argThat(List::isEmpty));
        verify(outboxService)
                .registrar(
                        eq("DiagnosticoConcluido"),
                        eq("os-1"),
                        argThat(payload ->
                                payload.containsKey("reservaId") && ((List<?>) payload.get("itens")).size() == 1));
    }

    @Test
    void deveRejeitarDiagnosticoVazioSemReservarEstoque() {
        TarefaExecucao tarefa = tarefaEmAndamento(TipoTarefa.DIAGNOSTICO, "os-2");
        when(tarefaRepository.buscarComItens(tarefa.getId())).thenReturn(Optional.of(tarefa));

        assertThrows(IllegalArgumentException.class, () -> service.concluirDiagnostico(tarefa.getId(), List.of()));

        verify(reservaService, never()).reservar(any(), anyList());
        verify(outboxService, never()).registrar(any(), any(), any());
    }

    @Test
    void deveRecusarInicioDeExecucaoSemConsumirReservaQuandoSimulacaoEstiverAtiva() {
        ReservaPecas reserva = new ReservaPecas("os-3", List.of(), List.of());
        when(reservaService.buscar("os-3")).thenReturn(reserva);
        when(tarefaRepository.findByOsIdAndTipo("os-3", TipoTarefa.REPARO)).thenReturn(Optional.empty());
        when(tarefaRepository.save(any(TarefaExecucao.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(simulacao.recusarInicioExecucao()).thenReturn(true);

        service.iniciarReparo("os-3", reserva.getId(), "cobranca-3", "ABC1234");

        verify(reservaService, never()).consumir(any());
        verify(outboxService)
                .registrar(
                        eq("ExecucaoRecusada"),
                        eq("os-3"),
                        argThat(payload -> payload.containsKey("motivo") && payload.containsKey("recusadaEm")));
    }

    @Test
    void deveResponderLiberacaoIdempotenteQuandoReservaNaoExiste() {
        when(reservaRepository.findByOsId("os-4")).thenReturn(Optional.empty());

        service.liberarReserva("os-4", null);

        verify(outboxService)
                .registrar(
                        eq("ReservaPecasLiberada"),
                        eq("os-4"),
                        argThat(payload -> ((List<?>) payload.get("itensLiberados")).isEmpty()));
        verify(reservaService, never()).liberar(any());
    }

    @Test
    void deveIniciarReparoConsumindoReservaEPublicandoEvento() {
        TarefaExecucao tarefa = new TarefaExecucao("os-5", TipoTarefa.REPARO, "ABC1234", null, 0);
        ReservaPecas reserva = new ReservaPecas("os-5", List.of(), List.of());
        when(tarefaRepository.buscarComItens(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(reservaService.consumir("os-5")).thenReturn(reserva);

        TarefaExecucao resultado = service.iniciarReparoDaFila(tarefa.getId());

        assertEquals(StatusTarefa.EM_ANDAMENTO, resultado.getStatus());
        verify(outboxService)
                .registrar(eq("ExecucaoIniciada"), eq("os-5"), argThat(payload -> payload.containsKey("reservaId")));
    }

    @Test
    void deveCancelarReparoRepondoReservaEPublicandoConfirmacao() {
        TarefaExecucao tarefa = tarefaEmAndamento(TipoTarefa.REPARO, "os-6");
        when(tarefaRepository.findByOsIdAndTipo("os-6", TipoTarefa.REPARO)).thenReturn(Optional.of(tarefa));

        service.cancelarReparo("os-6", "reserva-6");

        assertEquals(StatusTarefa.CANCELADA, tarefa.getStatus());
        verify(reservaService).cancelarExecucao("os-6", "reserva-6");
        verify(outboxService)
                .registrar(
                        eq("ExecucaoCancelada"),
                        eq("os-6"),
                        argThat(payload -> payload.get("reservaId").equals("reserva-6")));
    }

    @Test
    void cancelamentoQueChegaDepoisDaConclusaoReemiteConclusaoSemCompensarReserva() {
        TarefaExecucao tarefa = tarefaEmAndamento(TipoTarefa.REPARO, "os-7");
        tarefa.finalizarReparo();
        when(tarefaRepository.findByOsIdAndTipo("os-7", TipoTarefa.REPARO)).thenReturn(Optional.of(tarefa));

        service.cancelarReparo("os-7", "reserva-7");

        verify(outboxService).registrar(eq("ExecucaoFinalizada"), eq("os-7"), any());
        verify(reservaService, never()).cancelarExecucao(any(), any());
    }

    @Test
    void solicitarDiagnosticoReutilizaTarefaExistenteEListaPorStatus() {
        TarefaExecucao tarefa = new TarefaExecucao("os-8", TipoTarefa.DIAGNOSTICO, "ABC1234", null, 1);
        when(tarefaRepository.findByOsIdAndTipo("os-8", TipoTarefa.DIAGNOSTICO)).thenReturn(Optional.of(tarefa));
        when(tarefaRepository.findByStatus(eq(StatusTarefa.NA_FILA), any(Sort.class)))
                .thenReturn(List.of(tarefa));

        assertSame(tarefa, service.solicitarDiagnostico(new SolicitarDiagnosticoCommand("os-8", "ABC1234", null, 1)));
        assertEquals(List.of(tarefa), service.listar(StatusTarefa.NA_FILA));

        verify(tarefaRepository, never()).save(any(TarefaExecucao.class));
    }

    @Test
    void solicitarDiagnosticoCriaTarefaEBuscarInexistenteInformaErro() {
        when(tarefaRepository.findByOsIdAndTipo("os-9", TipoTarefa.DIAGNOSTICO)).thenReturn(Optional.empty());
        when(tarefaRepository.save(any(TarefaExecucao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TarefaExecucao tarefa =
                service.solicitarDiagnostico(new SolicitarDiagnosticoCommand("os-9", "ABC1234", "ruído", 2));

        assertEquals(TipoTarefa.DIAGNOSTICO, tarefa.getTipo());
        assertThrows(TarefaExecucaoService.TarefaNaoEncontradaException.class, () -> service.buscar("missing"));
    }

    @Test
    void iniciarDiagnosticoPublicaEventoERejeitaTarefaDeOutroTipo() {
        TarefaExecucao diagnostico = new TarefaExecucao("os-10", TipoTarefa.DIAGNOSTICO, "ABC1234", null, 0);
        when(tarefaRepository.buscarComItens(diagnostico.getId())).thenReturn(Optional.of(diagnostico));

        assertEquals(
                StatusTarefa.EM_ANDAMENTO,
                service.iniciarDiagnostico(diagnostico.getId()).getStatus());
        verify(outboxService).registrar(eq("DiagnosticoIniciado"), eq("os-10"), any());

        TarefaExecucao reparo = new TarefaExecucao("os-11", TipoTarefa.REPARO, "ABC1234", null, 0);
        when(tarefaRepository.buscarComItens(reparo.getId())).thenReturn(Optional.of(reparo));
        assertThrows(IllegalStateException.class, () -> service.iniciarDiagnostico(reparo.getId()));
    }

    @Test
    void concluirDiagnosticoResolveSnapshotDePecaERejeitaPecaInexistente() {
        TarefaExecucao tarefa = tarefaEmAndamento(TipoTarefa.DIAGNOSTICO, "os-12");
        Peca peca = new Peca("Filtro", null, new BigDecimal("8.00"), 10, 0);
        ReservaPecas reserva = new ReservaPecas("os-12", List.of(), List.of());
        when(tarefaRepository.buscarComItens(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(pecaRepository.findById(peca.getId())).thenReturn(Optional.of(peca));
        when(reservaService.reservar(eq("os-12"), anyList())).thenReturn(reserva);
        var item = new ItemDiagnosticoCommand(TipoItemDiagnostico.PECA, "Filtro", null, peca.getId(), 2);

        TarefaExecucao resultado = service.concluirDiagnostico(tarefa.getId(), List.of(item));

        assertEquals("Filtro", resultado.getItens().getFirst().getDescricao());
        assertEquals(new BigDecimal("16.00"), resultado.getItens().getFirst().getValorTotal());
        verify(reservaService)
                .reservar(
                        eq("os-12"),
                        argThat(itens ->
                                itens.size() == 1 && itens.getFirst().equals(new ItemReservaCommand(peca.getId(), 2))));

        TarefaExecucao semPeca = tarefaEmAndamento(TipoTarefa.DIAGNOSTICO, "os-13");
        when(tarefaRepository.buscarComItens(semPeca.getId())).thenReturn(Optional.of(semPeca));
        when(pecaRepository.findById("missing")).thenReturn(Optional.empty());
        var itemInexistente = new ItemDiagnosticoCommand(TipoItemDiagnostico.PECA, "Inexistente", null, "missing", 1);
        assertThrows(
                IllegalArgumentException.class,
                () -> service.concluirDiagnostico(semPeca.getId(), List.of(itemInexistente)));
    }

    @Test
    void iniciarReparoValidaDadosEConsomeReservaApenasUmaVez() {
        ReservaPecas reserva = new ReservaPecas("os-14", List.of(), List.of());
        TarefaExecucao tarefa = new TarefaExecucao("os-14", TipoTarefa.REPARO, "ABC1234", null, 0);
        when(reservaService.buscar("os-14")).thenReturn(reserva);
        when(tarefaRepository.findByOsIdAndTipo("os-14", TipoTarefa.REPARO)).thenReturn(Optional.of(tarefa));

        assertThrows(
                IllegalArgumentException.class, () -> service.iniciarReparo("os-14", reserva.getId(), " ", "ABC1234"));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.iniciarReparo("os-14", "outra-reserva", "cobranca-14", "ABC1234"));

        service.iniciarReparo("os-14", reserva.getId(), "cobranca-14", "ABC1234");

        assertEquals(StatusTarefa.EM_ANDAMENTO, tarefa.getStatus());
        verify(reservaService).consumir("os-14");
        verify(outboxService).registrar(eq("ExecucaoIniciada"), eq("os-14"), any());
    }

    @Test
    void filaEProgressoEFinalizacaoTratamEstadosDeReparo() {
        TarefaExecucao tarefa = new TarefaExecucao("os-15", TipoTarefa.REPARO, "ABC1234", null, 0);
        when(tarefaRepository.buscarComItens(tarefa.getId())).thenReturn(Optional.of(tarefa));
        ReservaPecas reserva = new ReservaPecas("os-15", List.of(), List.of());
        when(reservaService.consumir("os-15")).thenReturn(reserva);

        TarefaExecucao iniciada = service.iniciarReparoDaFila(tarefa.getId());
        assertEquals(StatusTarefa.EM_ANDAMENTO, iniciada.getStatus());
        assertSame(iniciada, service.iniciarReparoDaFila(tarefa.getId()));
        assertTrue(service.registrarProgresso(tarefa.getId(), "Substituição iniciada")
                .getProgresso()
                .contains("Substituição iniciada"));
        assertEquals(
                StatusTarefa.CONCLUIDA, service.finalizarReparo(tarefa.getId()).getStatus());
        verify(reservaService).consumir("os-15");
        verify(outboxService).registrar(eq("ExecucaoFinalizada"), eq("os-15"), any());
    }

    @Test
    void cancelamentoDeTarefaRecusadaNaoCompensaReservaEIdsObrigatoriosSaoValidados() {
        TarefaExecucao recusada = new TarefaExecucao("os-16", TipoTarefa.REPARO, "ABC1234", null, 0);
        recusada.recusar();
        when(tarefaRepository.findByOsIdAndTipo("os-16", TipoTarefa.REPARO)).thenReturn(Optional.of(recusada));

        service.cancelarReparo("os-16", "reserva-16");

        verify(outboxService).registrar(eq("ExecucaoRecusada"), eq("os-16"), any());
        verify(reservaService, never()).cancelarExecucao(any(), any());
        assertThrows(IllegalArgumentException.class, () -> service.cancelarReparo(" ", "reserva-16"));
    }

    @Test
    void liberarReservaValidaIdentidadeERejeitaReservaConsumida() {
        Peca peca = new Peca("Filtro", null, new BigDecimal("8.00"), 10, 0);
        ReservaPecas reservada = new ReservaPecas("os-17", List.of(peca), List.of(2));
        when(reservaRepository.findByOsId("os-17")).thenReturn(Optional.of(reservada));

        service.liberarReserva("os-17", reservada.getId());
        verify(reservaService).liberar("os-17");
        verify(outboxService)
                .registrar(
                        eq("ReservaPecasLiberada"),
                        eq("os-17"),
                        argThat(payload -> ((List<?>) payload.get("itensLiberados")).size() == 1));

        ReservaPecas consumida = new ReservaPecas("os-18", List.of(), List.of());
        consumida.consumir();
        when(reservaRepository.findByOsId("os-18")).thenReturn(Optional.of(consumida));
        assertThrows(IllegalStateException.class, () -> service.liberarReserva("os-18", consumida.getId()));
        assertThrows(IllegalArgumentException.class, () -> service.liberarReserva("os-17", "id-divergente"));
    }

    private TarefaExecucao tarefaEmAndamento(TipoTarefa tipo, String osId) {
        TarefaExecucao tarefa = new TarefaExecucao(osId, tipo, "ABC1234", null, 0);
        tarefa.iniciar();
        return tarefa;
    }
}

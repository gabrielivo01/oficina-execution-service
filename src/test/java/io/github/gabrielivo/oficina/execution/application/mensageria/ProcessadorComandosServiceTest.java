package io.github.gabrielivo.oficina.execution.application.mensageria;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.execution.application.execucao.TarefaExecucaoService;
import io.github.gabrielivo.oficina.execution.domain.mensageria.MensagemProcessadaRepository;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessadorComandosServiceTest {

    @Mock
    private MensagemProcessadaRepository inbox;

    @Mock
    private TarefaExecucaoService tarefas;

    private ProcessadorComandosService service;

    @BeforeEach
    void setUp() {
        service = new ProcessadorComandosService(inbox, tarefas);
    }

    @Test
    void deveIgnorarComandoJaProcessado() {
        var mensagem = new EnvelopeMensagem("m-1", "SolicitarDiagnostico", "os-1", "os-1", Instant.now(), Map.of());
        when(inbox.existsById("m-1")).thenReturn(true);

        service.processar(mensagem);

        verify(tarefas, never()).solicitarDiagnostico(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveRejeitarTipoDeComandoDesconhecido() {
        var mensagem = new EnvelopeMensagem("m-2", "TipoInvalido", "os-2", "os-2", Instant.now(), Map.of());
        when(inbox.existsById("m-2")).thenReturn(false);

        assertThrows(ProcessadorComandosService.MensagemNaoSuportadaException.class, () -> service.processar(mensagem));
    }

    @Test
    void deveDespacharComandoDeCancelamentoDeExecucao() {
        var mensagem = new EnvelopeMensagem(
                "m-3", "CancelarExecucao", "os-3", "os-3", Instant.now(), Map.of("reservaId", "reserva-3"));
        when(inbox.existsById("m-3")).thenReturn(false);

        service.processar(mensagem);

        verify(tarefas).cancelarReparo("os-3", "reserva-3");
    }
}

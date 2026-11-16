package io.github.gabrielivo.oficina.execution.application.mensageria;

import io.github.gabrielivo.oficina.execution.application.execucao.SolicitarDiagnosticoCommand;
import io.github.gabrielivo.oficina.execution.application.execucao.TarefaExecucaoService;
import io.github.gabrielivo.oficina.execution.domain.mensageria.MensagemProcessada;
import io.github.gabrielivo.oficina.execution.domain.mensageria.MensagemProcessadaRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessadorComandosService {

    private final MensagemProcessadaRepository inbox;
    private final TarefaExecucaoService tarefas;

    public ProcessadorComandosService(MensagemProcessadaRepository inbox, TarefaExecucaoService tarefas) {
        this.inbox = inbox;
        this.tarefas = tarefas;
    }

    @Transactional
    public void processar(EnvelopeMensagem mensagem) {
        if (mensagem.messageId() == null || mensagem.messageId().isBlank()) {
            throw new IllegalArgumentException("messageId é obrigatório.");
        }
        if (inbox.existsById(mensagem.messageId())) {
            return;
        }
        switch (mensagem.type()) {
            case "SolicitarDiagnostico" -> processarSolicitacao(mensagem);
            case "IniciarExecucao" -> processarInicioExecucao(mensagem);
            case "CancelarExecucao" -> processarCancelamentoExecucao(mensagem);
            case "LiberarReservaPecas" ->
                tarefas.liberarReserva(
                        mensagem.osId(), (String) mensagem.payload().get("reservaId"));
            default -> throw new MensagemNaoSuportadaException(mensagem.type());
        }
        inbox.save(new MensagemProcessada(mensagem.messageId()));
    }

    private void processarSolicitacao(EnvelopeMensagem mensagem) {
        exigirPayload(mensagem);
        Map<String, Object> payload = mensagem.payload();
        Map<?, ?> veiculo = (Map<?, ?>) payload.get("veiculo");
        String placa = veiculo == null ? null : (String) veiculo.get("placa");
        tarefas.solicitarDiagnostico(
                new SolicitarDiagnosticoCommand(mensagem.osId(), placa, (String) payload.get("observacao"), 0));
    }

    private void processarInicioExecucao(EnvelopeMensagem mensagem) {
        exigirPayload(mensagem);
        Map<String, Object> payload = mensagem.payload();
        tarefas.iniciarReparo(
                mensagem.osId(), (String) payload.get("reservaId"), (String) payload.get("cobrancaId"), (String)
                        payload.get("placa"));
    }

    private void processarCancelamentoExecucao(EnvelopeMensagem mensagem) {
        exigirPayload(mensagem);
        tarefas.cancelarReparo(mensagem.osId(), (String) mensagem.payload().get("reservaId"));
    }

    private void exigirPayload(EnvelopeMensagem mensagem) {
        if (mensagem.osId() == null || mensagem.osId().isBlank() || mensagem.payload() == null) {
            throw new IllegalArgumentException("osId e payload são obrigatórios.");
        }
    }

    public static class MensagemNaoSuportadaException extends RuntimeException {
        public MensagemNaoSuportadaException(String type) {
            super("Comando não suportado pelo Execution Service: " + type);
        }
    }
}

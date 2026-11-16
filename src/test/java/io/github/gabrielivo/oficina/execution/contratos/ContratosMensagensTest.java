package io.github.gabrielivo.oficina.execution.contratos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabrielivo.oficina.execution.application.mensageria.EnvelopeMensagem;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ContratosMensagensTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void deveDesserializarOsContratosDoCatalogoDeMensagens() throws IOException {
        List<String> tipos = List.of(
                "SolicitarDiagnostico",
                "IniciarExecucao",
                "LiberarReservaPecas",
                "DiagnosticoIniciado",
                "DiagnosticoConcluido",
                "ExecucaoIniciada",
                "ExecucaoRecusada",
                "ExecucaoFinalizada",
                "ReservaPecasLiberada");

        for (String tipo : tipos) {
            try (InputStream json = getClass().getResourceAsStream("/contratos/" + tipo + ".json")) {
                assertNotNull(json, "Contrato ausente: " + tipo);
                EnvelopeMensagem envelope = jsonMapper.readValue(json, EnvelopeMensagem.class);
                assertEquals(tipo, envelope.type());
                assertNotNull(envelope.messageId());
                assertNotNull(envelope.osId());
                assertNotNull(envelope.occurredAt());
                assertTrue(envelope.payload() != null && !envelope.payload().isEmpty());
            }
        }
    }
}

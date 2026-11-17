package io.github.gabrielivo.oficina.execution.infrastructure.observabilidade;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O Prometheus coleta {@code /actuator/prometheus} sem token (ServiceMonitor), com a segurança ligada como no cluster.
 * O histograma de {@code http.server.requests} alimenta o painel de latência p95 do dashboard do Execution.
 */
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:execution_prometheus;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.rabbitmq.listener.simple.auto-startup=false",
            "app.mensageria.outbox.publicador-habilitado=false",
            "management.tracing.export.otlp.enabled=false",
            "management.tracing.sampling.probability=0.0",
            "jwt.secret=segredo-de-teste-com-pelo-menos-256-bits-para-hs256-ok"
        })
@AutoConfigureMockMvc
class EndpointPrometheusTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void scrapeSemTokenDeveExporAsMetricasComHistogramaDeLatencia() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_memory_used_bytes")))
                .andExpect(content().string(containsString("http_server_requests_seconds_bucket")));
    }
}

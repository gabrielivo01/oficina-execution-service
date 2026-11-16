package io.github.gabrielivo.oficina.execution;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.gabrielivo.oficina.execution.infrastructure.mensageria.ConsumidorComandos;
import io.github.gabrielivo.oficina.execution.presentation.execucao.TarefaController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Garante que a aplicação monta sem erro de wiring: H2 em modo PostgreSQL com as migrations Flyway, listeners AMQP
 * sem iniciar (não há broker), publicador do outbox e exportação de traces desligados. Segurança fica ligada, como em
 * produção.
 */
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:execution_ctx;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.rabbitmq.listener.simple.auto-startup=false",
            "app.mensageria.outbox.publicador-habilitado=false",
            "management.tracing.export.otlp.enabled=false",
            "management.tracing.sampling.probability=0.0",
            "jwt.secret=segredo-de-teste-com-pelo-menos-256-bits-para-hs256-ok"
        })
class ContextoAplicacaoTest {

    @Autowired
    private ApplicationContext contexto;

    @Test
    void contextoDeveCarregarComOsComponentesPrincipais() {
        assertThat(contexto.getBean(TarefaController.class)).isNotNull();
        assertThat(contexto.getBean(ConsumidorComandos.class)).isNotNull();
    }
}

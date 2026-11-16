package io.github.gabrielivo.oficina.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.gabrielivo.oficina.execution.domain.execucao.ItemDiagnostico;
import io.github.gabrielivo.oficina.execution.domain.execucao.StatusTarefa;
import io.github.gabrielivo.oficina.execution.domain.execucao.TarefaExecucao;
import io.github.gabrielivo.oficina.execution.domain.execucao.TarefaExecucaoRepository;
import io.github.gabrielivo.oficina.execution.domain.execucao.TipoItemDiagnostico;
import io.github.gabrielivo.oficina.execution.domain.execucao.TipoTarefa;
import io.github.gabrielivo.oficina.execution.domain.mensageria.MensagemOutbox;
import io.github.gabrielivo.oficina.execution.domain.mensageria.MensagemOutboxRepository;
import io.github.gabrielivo.oficina.execution.domain.peca.Peca;
import io.github.gabrielivo.oficina.execution.domain.peca.PecaRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * Repositórios com consulta própria contra H2 em modo PostgreSQL, com o schema criado pelas migrations Flyway (inclui
 * o seed de peças) e validado pelo Hibernate ({@code ddl-auto=validate}). Sem Docker (ADR 0013 do oficina).
 */
@DataJpaTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:execution_jpa;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
            "spring.datasource.username=sa",
            "spring.datasource.password="
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoriosJpaTest {

    private static final String FILTRO_DE_OLEO = "c0a8012e-0003-4000-8000-000000000003";
    private static final String PASTILHA = "c0a8012e-0003-4000-8000-000000000004";

    @Autowired
    private PecaRepository pecas;

    @Autowired
    private TarefaExecucaoRepository tarefas;

    @Autowired
    private MensagemOutboxRepository outbox;

    @Autowired
    private TestEntityManager em;

    @Test
    void migrationsDevemCriarOSchemaEOSeedDePecas() {
        assertThat(pecas.count()).isEqualTo(5);
        assertThat(pecas.findById(FILTRO_DE_OLEO))
                .get()
                .extracting(Peca::getNome)
                .isEqualTo("Filtro de Óleo");
    }

    @Test
    void deveListarEstoqueBaixoECriticoOrdenadosPorNome() {
        pecas.save(new Peca("Correia dentada", null, new BigDecimal("80.00"), 4, 4));
        pecas.save(new Peca("Amortecedor", null, new BigDecimal("220.25"), 1, 6));
        em.flush();

        assertThat(pecas.buscarEstoqueBaixo())
                .extracting(Peca::getNome)
                .containsExactly("Amortecedor", "Correia dentada");
        assertThat(pecas.buscarEstoqueCritico()).extracting(Peca::getNome).containsExactly("Amortecedor");
    }

    @Test
    void deveBuscarAsPecasDaReservaPorIdOrdenadasPorNome() {
        assertThat(pecas.buscarParaReserva(List.of(PASTILHA, FILTRO_DE_OLEO, "inexistente")))
                .extracting(Peca::getNome)
                .containsExactly("Filtro de Óleo", "Pastilha de Freio");
    }

    @Test
    void atualizacaoConcorrenteDoEstoqueDeveFalharPelaVersao() {
        Peca peca = pecas.findById(FILTRO_DE_OLEO).orElseThrow();
        em.detach(peca);
        Peca outraLeitura = pecas.findById(FILTRO_DE_OLEO).orElseThrow();
        outraLeitura.reduzirEstoque(1);
        pecas.saveAndFlush(outraLeitura);

        peca.reduzirEstoque(2);
        assertThatThrownBy(() -> pecas.saveAndFlush(peca)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void deveBuscarTarefaPorOsETipoComItensEFilaPorStatus() {
        TarefaExecucao diagnostico = new TarefaExecucao("os-1", TipoTarefa.DIAGNOSTICO, "ABC1D23", "Barulho", 0);
        diagnostico.iniciar();
        diagnostico.concluirDiagnostico(List.of(
                new ItemDiagnostico(TipoItemDiagnostico.SERVICO, "Troca", new BigDecimal("150.00"), null, 1),
                new ItemDiagnostico(TipoItemDiagnostico.PECA, "Filtro", new BigDecimal("25.50"), FILTRO_DE_OLEO, 2)));
        tarefas.save(diagnostico);
        tarefas.save(new TarefaExecucao("os-2", TipoTarefa.DIAGNOSTICO, "XYZ9A88", null, 5));
        em.flush();
        em.clear();

        TarefaExecucao encontrada =
                tarefas.findByOsIdAndTipo("os-1", TipoTarefa.DIAGNOSTICO).orElseThrow();
        assertThat(tarefas.buscarComItens(encontrada.getId()).orElseThrow().getItens())
                .hasSize(2);
        assertThat(tarefas.findByOsIdAndTipo("os-1", TipoTarefa.REPARO)).isEmpty();
        assertThat(tarefas.findByStatus(StatusTarefa.NA_FILA, Sort.by("prioridade")))
                .extracting(TarefaExecucao::getOsId)
                .containsExactly("os-2");
    }

    @Test
    void deveListarSoAsMensagensPendentesDoOutboxNaOrdemDeGravacao() {
        MensagemOutbox primeira = outbox.save(new MensagemOutbox("A", "oficina.eventos", "rk", "{}", null));
        MensagemOutbox enviada = new MensagemOutbox("B", "oficina.eventos", "rk", "{}", null);
        enviada.marcarEnviada();
        outbox.save(enviada);
        MensagemOutbox terceira = outbox.save(new MensagemOutbox("C", "oficina.eventos", "rk", "{}", null));
        em.flush();

        assertThat(outbox.findByEnviadoEmIsNullOrderByCriadoEmAsc(PageRequest.of(0, 10)))
                .extracting(MensagemOutbox::getId)
                .containsExactly(primeira.getId(), terceira.getId());
    }
}

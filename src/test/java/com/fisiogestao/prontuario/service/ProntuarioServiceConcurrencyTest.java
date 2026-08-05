package com.fisiogestao.prontuario.service;

import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.repository.ClienteRepository;
import com.fisiogestao.prontuario.ProntuarioSoapEntity;
import com.fisiogestao.prontuario.TipoRegistroProntuario;
import com.fisiogestao.prontuario.form.AvaliacaoForm;
import com.fisiogestao.prontuario.repository.ProntuarioSoapRepository;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;

/**
 * Reproduz de verdade — com duas transações reais e concorrentes, contra o mesmo tipo de
 * índice único criado na V50 (aqui recriado manualmente em H2 via uma coluna computada, já
 * que o Flyway fica desligado no perfil de teste, o {@code ddl-auto=create-drop} só gera
 * schema a partir das anotações da entidade, e H2 2.3 não suporta a sintaxe de índice
 * parcial "CREATE UNIQUE INDEX ... WHERE" usada no Postgres real) — a condição de corrida
 * do índice {@code uq_prontuario_soap_avaliacao_por_cliente}: duas requisições de criação
 * de avaliação para o mesmo paciente leem "sem avaliação existente" antes de qualquer
 * commit e tentam inserir ao mesmo tempo.
 *
 * <p>A intercalação é forçada, sem depender de sorte de agendamento de threads: o método de
 * leitura ({@link ProntuarioSoapRepository#findFirstByClienteIdAndTipoRegistro}) é
 * substituído por uma implementação equivalente que trava as duas threads num
 * {@link CyclicBarrier} logo depois de ler — ou seja, a segunda thread só prossegue para o
 * INSERT depois que a primeira já leu "vazio" e ainda não inseriu nada, exatamente a janela
 * de corrida que existe em produção.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProntuarioServiceConcurrencyTest {

    @Autowired
    private ProntuarioService prontuarioService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProntuarioSoapRepository prontuarioSoapRepository;

    @MockitoSpyBean
    private ProntuarioSoapRepository prontuarioSoapRepositorySpy;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    private ClienteEntity paciente;

    @BeforeEach
    void setUp() {
        // H2 2.3 não suporta "CREATE UNIQUE INDEX ... WHERE" (índice parcial, sintaxe usada na
        // V50/Postgres real). Reproduzimos a mesma regra com uma coluna computada: NULL para
        // linhas que não são AVALIACAO, e um índice único sobre ela — em SQL padrão, múltiplos
        // NULLs não colidem entre si, então evoluções (várias por cliente) continuam livres,
        // só avaliações (uma por cliente) ficam sujeitas à unicidade.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.createNativeQuery(
                    "ALTER TABLE prontuario_soap ADD COLUMN IF NOT EXISTS avaliacao_cliente_marker UUID "
                            + "AS (CASE WHEN tipo_registro = 'AVALIACAO' THEN cliente_id ELSE NULL END)").executeUpdate();
            entityManager.createNativeQuery(
                    "CREATE UNIQUE INDEX IF NOT EXISTS uq_prontuario_soap_avaliacao_por_cliente "
                            + "ON prontuario_soap (avaliacao_cliente_marker)").executeUpdate();
        });

        paciente = clienteRepository.save(ClienteEntity.builder()
                .nomeCompleto("Paciente Concorrencia Avaliacao")
                .telefone("65999990005")
                .email("paciente.concorrencia.avaliacao@example.com")
                .ativo(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        if (paciente != null) {
            prontuarioSoapRepository.findByClienteIdOrderByCreatedAtDesc(paciente.getId())
                    .forEach(registro -> prontuarioSoapRepository.deleteById(registro.getId()));
            clienteRepository.deleteById(paciente.getId());
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.createNativeQuery("DROP INDEX IF EXISTS uq_prontuario_soap_avaliacao_por_cliente").executeUpdate();
            entityManager.createNativeQuery(
                    "ALTER TABLE prontuario_soap DROP COLUMN IF EXISTS avaliacao_cliente_marker").executeUpdate();
        });
    }

    @Test
    void confirmaQueIndiceUnicoEstaAtivoNoH2DeTeste() {
        assertTrue(indexExiste(), "Pré-condição do teste: o índice único equivalente precisa existir no H2 de teste.");
    }

    @Test
    void duasCriacoesConcorrentesDaMesmaAvaliacaoResultamEmUmaUnicaLinhaEBusinessExceptionNaSegunda() throws Exception {
        CyclicBarrier barreiraLeitura = new CyclicBarrier(2);

        // Substitui a leitura por uma implementação equivalente (mesma consulta JPQL) que
        // trava as duas threads logo após ler — assim nenhuma das duas prossegue para o
        // INSERT antes que a outra também já tenha lido "sem avaliação existente".
        doAnswer(invocation -> {
            Optional<ProntuarioSoapEntity> resultado = entityManager.createQuery(
                            "select r from ProntuarioSoapEntity r "
                                    + "where r.cliente.id = :clienteId and r.tipoRegistro = :tipo",
                            ProntuarioSoapEntity.class)
                    .setParameter("clienteId", paciente.getId())
                    .setParameter("tipo", TipoRegistroProntuario.AVALIACAO)
                    .getResultStream()
                    .findFirst();
            barreiraLeitura.await(5, TimeUnit.SECONDS);
            return resultado;
        }).when(prontuarioSoapRepositorySpy)
                .findFirstByClienteIdAndTipoRegistro(paciente.getId(), TipoRegistroProntuario.AVALIACAO);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Resultado> futuroA = executor.submit(() -> salvarEClassificar("Avaliação da requisição A."));
            Future<Resultado> futuroB = executor.submit(() -> salvarEClassificar("Avaliação da requisição B."));

            Resultado resultadoA = futuroA.get(10, TimeUnit.SECONDS);
            Resultado resultadoB = futuroB.get(10, TimeUnit.SECONDS);

            long sucessos = List.of(resultadoA, resultadoB).stream().filter(Resultado::sucesso).count();
            long conflitos = List.of(resultadoA, resultadoB).stream().filter(r -> !r.sucesso()).count();

            assertEquals(1, sucessos, "Exatamente uma das duas requisições concorrentes deve criar a avaliação.");
            assertEquals(1, conflitos, "A outra deve receber um conflito amigável, não uma exceção crua.");

            Resultado conflito = resultadoA.sucesso() ? resultadoB : resultadoA;
            assertInstanceOf(BusinessException.class, conflito.erro());
            assertEquals(
                    "Esta avaliação já foi salva em outra requisição. Atualize a página e confira o conteúdo antes de salvar novamente.",
                    conflito.erro().getMessage());

            List<ProntuarioSoapEntity> registrosPersistidos =
                    prontuarioSoapRepository.findByClienteIdOrderByCreatedAtDesc(paciente.getId());
            assertEquals(1, registrosPersistidos.size(), "O índice único deve garantir uma única avaliação persistida.");
        } finally {
            executor.shutdownNow();
        }
    }

    private Resultado salvarEClassificar(String texto) {
        AvaliacaoForm form = new AvaliacaoForm();
        form.setClienteId(paciente.getId());
        form.setTexto(texto);
        try {
            prontuarioService.salvarAvaliacao(form, "fisio-concorrencia");
            return new Resultado(true, null);
        } catch (RuntimeException ex) {
            return new Resultado(false, ex);
        }
    }

    private boolean indexExiste() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            @SuppressWarnings("unchecked")
            List<Object[]> linhas = entityManager.createNativeQuery(
                            "SELECT table_name, index_name FROM information_schema.indexes")
                    .getResultList();
            return linhas.stream().anyMatch(linha ->
                    String.valueOf(linha[1]).equalsIgnoreCase("uq_prontuario_soap_avaliacao_por_cliente"));
        });
    }

    private record Resultado(boolean sucesso, RuntimeException erro) {
    }
}

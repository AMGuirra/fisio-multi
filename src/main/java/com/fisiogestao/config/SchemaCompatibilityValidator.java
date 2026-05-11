package com.fisiogestao.config;

import com.fisiogestao.pacientes.service.PacienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Valida a compatibilidade do schema do banco na inicialização da aplicação.
 *
 * <p>Garante que colunas críticas (como cliente.id e solicitacao_agendamento.cliente_id)
 * sejam do tipo UUID, detectando bancos legados que não passaram pelas migrations corretivas
 * antes de aceitar tráfego.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SchemaCompatibilityValidator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PacienteService pacienteService;

    @Override
    public void run(ApplicationArguments args) {
        validarColuna("cliente", "id",
                "Banco incompatível: cliente.id está como %s (esperado: uuid). "
                        + "Aplique o script docs/sql/correcao_cliente_id_uuid.sql antes de iniciar.");

        validarColuna("solicitacao_agendamento", "cliente_id",
                "Banco incompatível: solicitacao_agendamento.cliente_id está como %s (esperado: uuid). "
                        + "Aplique o script docs/sql/correcao_cliente_id_uuid.sql antes de iniciar.");

        // Marca as flags no service para evitar revalidação em runtime
        pacienteService.marcarSchemaValidado();

        log.info("Compatibilidade de schema validada com sucesso.");
    }

    private void validarColuna(String tabela, String coluna, String mensagemErro) {
        try {
            String tipo = jdbcTemplate.queryForObject(
                    """
                    SELECT data_type
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name   = ?
                      AND column_name  = ?
                    """,
                    String.class,
                    tabela,
                    coluna
            );

            if (tipo == null || tipo.isBlank()) {
                log.warn("Coluna {}.{} não encontrada no schema public — pode ser banco de testes ou schema diferente.", tabela, coluna);
                return;
            }

            if (!"uuid".equalsIgnoreCase(tipo)) {
                throw new IllegalStateException(mensagemErro.formatted(tipo));
            }

        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            // Em ambientes de teste (H2) a tabela pode não existir ainda — apenas loga.
            log.warn("Não foi possível validar {}.{} — ignorando em ambiente de teste: {}", tabela, coluna, ex.getMessage());
        }
    }
}

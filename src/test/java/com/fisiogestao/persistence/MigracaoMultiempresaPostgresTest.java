package com.fisiogestao.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Requisito 22: a migração vincula os registros legados à "Clínica Principal".
 *
 * <p>Executa as migrations reais contra PostgreSQL. Para rodar, defina
 * {@code FISIO_PG_TEST_URL} (servidor, ex.: {@code jdbc:postgresql://localhost:5432/postgres}),
 * {@code FISIO_PG_TEST_USER} e {@code FISIO_PG_TEST_PASSWORD}. O teste cria um banco temporário
 * exclusivo e o remove ao final — nenhum banco existente é alterado.</p>
 */
@EnabledIfEnvironmentVariable(named = "FISIO_PG_TEST_URL", matches = ".+")
class MigracaoMultiempresaPostgresTest {

    private static final String LEGADA = "00000000-0000-0000-0000-000000000001";
    private static final String BANCO = "fisio_migracao_teste_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

    private static String urlServidor;
    private static String usuario;
    private static String senha;

    @BeforeAll
    static void criarBanco() throws SQLException {
        urlServidor = System.getenv("FISIO_PG_TEST_URL");
        usuario = System.getenv().getOrDefault("FISIO_PG_TEST_USER", "postgres");
        senha = System.getenv().getOrDefault("FISIO_PG_TEST_PASSWORD", "postgres");
        try (Connection c = DriverManager.getConnection(urlServidor, usuario, senha); Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE " + BANCO);
        }
    }

    @AfterAll
    static void removerBanco() throws SQLException {
        try (Connection c = DriverManager.getConnection(urlServidor, usuario, senha); Statement s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS " + BANCO + " WITH (FORCE)");
        }
    }

    private static String urlBanco() {
        int barra = urlServidor.lastIndexOf('/');
        int interrogacao = urlServidor.indexOf('?', barra);
        String sufixo = interrogacao >= 0 ? urlServidor.substring(interrogacao) : "";
        return urlServidor.substring(0, barra + 1) + BANCO + sufixo;
    }

    private static Flyway flyway(String alvo) {
        return Flyway.configure()
                .dataSource(urlBanco(), usuario, senha)
                .locations("classpath:db/migration")
                .table("flyway_schema_history_fisiogestao")
                .target(alvo)
                .load();
    }

    @Test
    void migracaoVinculaRegistrosLegadosAClinicaPrincipalEAplicaIsolamento() throws SQLException {
        flyway("53").migrate();

        try (Connection c = DriverManager.getConnection(urlBanco(), usuario, senha); Statement s = c.createStatement()) {
            s.execute("""
                    INSERT INTO usuario_sistema (username, password_hash, role, email, nome, tema_interface)
                    VALUES ('maria', 'x', 'ADMIN', 'maria@legado.test', 'Maria', 'LIGHT'),
                           ('joao', 'x', 'RECEPCAO', 'joao@legado.test', 'João', 'LIGHT')""");
            s.execute("""
                    INSERT INTO cliente (id, nome_completo, cpf, portal_token)
                    VALUES ('11111111-1111-1111-1111-111111111111', 'Paciente Legado', '52998224725', gen_random_uuid())""");
            s.execute("""
                    INSERT INTO plano_sessao (id, cliente_id, nome_plano, sessoes_contratadas, sessoes_consumidas, valor_total, data_inicio, status)
                    VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'Plano', 10, 0, 100, CURRENT_DATE, 'ATIVO')""");
            s.execute("""
                    INSERT INTO sessao (cliente_id, plano_sessao_id, data_hora_inicio, data_hora_fim, status, tipo_agendamento)
                    VALUES ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', now(), now() + interval '1 hour', 'AGENDADA', 'PLANO')""");
            s.execute("INSERT INTO lancamento_financeiro (tipo, status, descricao, categoria, valor, data_recebimento) VALUES ('RECEITA', 'PAGO', 'x', 'x', 10, now())");
            s.execute("INSERT INTO auditoria_sistema (modulo, entidade, tipo_acao, origem_evento) VALUES ('PACIENTES', 'cliente', 'CREATE', 'MANUAL')");
            s.execute("INSERT INTO user_role_menu_permission (role, menu_key) VALUES ('RECEPCAO', 'AGENDA')");
        }

        flyway("latest").migrate();

        try (Connection c = DriverManager.getConnection(urlBanco(), usuario, senha); Statement s = c.createStatement()) {
            assertEquals("Clínica Principal", texto(s, "SELECT nome FROM empresa WHERE id = '" + LEGADA + "'"));
            assertEquals("ATIVO", texto(s, "SELECT status FROM empresa WHERE id = '" + LEGADA + "'"));
            for (String tabela : new String[]{"usuario_sistema", "cliente", "plano_sessao", "sessao", "lancamento_financeiro",
                    "auditoria_sistema", "user_role_menu_permission", "configuracao_clinica"}) {
                assertEquals(0, numero(s, "SELECT COUNT(*) FROM " + tabela + " WHERE empresa_id IS DISTINCT FROM '" + LEGADA + "'"),
                        "Todos os registros legados de " + tabela + " devem pertencer à Clínica Principal.");
                assertTrue(numero(s, "SELECT COUNT(*) FROM " + tabela) > 0, "Tabela " + tabela + " deveria ter dados.");
            }
            assertEquals(1, numero(s, "SELECT COUNT(*) FROM usuario_sistema WHERE role = 'ADMIN_CLINICA'"),
                    "O antigo ADMIN vira ADMIN_CLINICA.");
            assertEquals(1, numero(s, "SELECT COUNT(*) FROM configuracao_plataforma"));

            // Integridade entre empresas: paciente de outra empresa não pode ser referenciado.
            s.execute("INSERT INTO empresa (id, nome, slug, status) VALUES ('33333333-3333-3333-3333-333333333333', 'B', 'clinica-b', 'ATIVO')");
            assertThrows(SQLException.class, () -> s.execute("""
                    INSERT INTO sessao (empresa_id, cliente_id, data_hora_inicio, data_hora_fim, status, tipo_agendamento)
                    VALUES ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', now(), now(), 'AGENDADA', 'AVULSO')"""));
            // Mesmo username em empresas diferentes é permitido; superadmin sem empresa também.
            s.execute("INSERT INTO usuario_sistema (username, password_hash, role, nome, tema_interface, empresa_id) VALUES ('maria', 'x', 'ADMIN_CLINICA', 'Maria B', 'LIGHT', '33333333-3333-3333-3333-333333333333')");
            s.execute("INSERT INTO usuario_sistema (username, password_hash, role, nome, tema_interface) VALUES ('admin', 'x', 'SUPER_ADMIN', 'Super', 'LIGHT')");
            assertThrows(SQLException.class, () -> s.execute(
                    "INSERT INTO usuario_sistema (username, password_hash, role, nome, tema_interface) VALUES ('sem-empresa', 'x', 'RECEPCAO', 'X', 'LIGHT')"));
        }
    }

    private static String texto(Statement s, String sql) throws SQLException {
        try (ResultSet rs = s.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static long numero(Statement s, String sql) throws SQLException {
        try (ResultSet rs = s.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}

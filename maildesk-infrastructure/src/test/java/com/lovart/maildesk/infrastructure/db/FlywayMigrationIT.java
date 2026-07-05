package com.lovart.maildesk.infrastructure.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Spins up postgres:16-alpine via Testcontainers, runs the Flyway migrations from
 * {@code classpath:db/migration}, and asserts the resulting schema is complete.
 * <p>
 * Skipped (not failed) when no Docker daemon is reachable, so {@code mvn verify}
 * stays green on machines without Docker while still running for real in CI.
 */
class FlywayMigrationIT {

    private static final int EXPECTED_MIGRATION_COUNT = 14;

    private static final List<String> EXPECTED_TABLES = List.of(
            "tenants",
            "profiles",
            "kols",
            "emails",
            "email_threads",
            "email_templates",
            "scheduled_emails",
            "actions",
            "integration_credentials",
            "sync_jobs",
            "ai_usage_log"
    );

    private static final List<String> EXPECTED_ENUMS = List.of(
            "kol_stage",
            "kol_status",
            "platform",
            "email_direction",
            "action_type"
    );

    @BeforeAll
    static void requireDocker() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available; skipping Flyway migration integration test");
    }

    @Test
    void migrationsApplyCleanlyAndProduceExpectedSchema() throws Exception {
        try (PostgreSQLContainer<?> postgres =
                     new PostgreSQLContainer<>("postgres:16-alpine")
                             .withDatabaseName("maildesk")
                             .withUsername("maildesk")
                             .withPassword("maildesk_local")) {
            postgres.start();

            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .load();

            MigrateResult result = flyway.migrate();

            assertThat(result.success).isTrue();
            assertThat(result.migrationsExecuted).isEqualTo(EXPECTED_MIGRATION_COUNT);
            assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("15");

            try (Connection conn = postgres.createConnection("")) {
                assertExpectedTablesExist(conn);
                assertExpectedEnumsExist(conn);
                assertDefaultTenantSeeded(conn);
                assertEveryBusinessTableHasMultiTenantColumns(conn);
                assertKolStageHasNewValues(conn);
                assertNormalizedEmailIsGenerated(conn);
            }
        }
    }

    private void assertExpectedTablesExist(Connection conn) throws Exception {
        Set<String> actual = new HashSet<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")) {
            while (rs.next()) {
                actual.add(rs.getString(1));
            }
        }
        assertThat(actual).contains(EXPECTED_TABLES.toArray(new String[0]));
    }

    private void assertExpectedEnumsExist(Connection conn) throws Exception {
        Set<String> actual = new HashSet<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT typname FROM pg_type WHERE typtype = 'e'")) {
            while (rs.next()) {
                actual.add(rs.getString(1));
            }
        }
        assertThat(actual).contains(EXPECTED_ENUMS.toArray(new String[0]));
    }

    private void assertDefaultTenantSeeded(Connection conn) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT count(*) FROM tenants WHERE id = '00000000-0000-0000-0000-000000000001'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }

    private void assertEveryBusinessTableHasMultiTenantColumns(Connection conn) throws Exception {
        String[] auditColumns = {"created_at", "updated_at", "created_by", "updated_by", "deleted_at", "version"};
        for (String table : EXPECTED_TABLES) {
            Set<String> columns = columnsOf(conn, table);
            // tenants table is exempt from tenant_id (it IS the tenant).
            if (!"tenants".equals(table)) {
                assertThat(columns)
                        .as("table %s must have tenant_id", table)
                        .contains("tenant_id");
            }
            for (String col : auditColumns) {
                assertThat(columns)
                        .as("table %s must have audit column %s", table, col)
                        .contains(col);
            }
        }
    }

    private void assertKolStageHasNewValues(Connection conn) throws Exception {
        Set<String> values = new HashSet<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT e.enumlabel FROM pg_enum e "
                             + "JOIN pg_type t ON t.oid = e.enumtypid WHERE t.typname = 'kol_stage'")) {
            while (rs.next()) {
                values.add(rs.getString(1));
            }
        }
        assertThat(values).contains("outreach", "replied", "producing", "declined");
    }

    private void assertNormalizedEmailIsGenerated(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO kols (tenant_id, email) "
                    + "VALUES ('00000000-0000-0000-0000-000000000001', '  Test@Example.COM ')");
            try (ResultSet rs = st.executeQuery(
                    "SELECT normalized_email FROM kols WHERE email = '  Test@Example.COM '")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("test@example.com");
            }
        }
    }

    private Set<String> columnsOf(Connection conn, String table) throws Exception {
        Set<String> columns = new HashSet<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT column_name FROM information_schema.columns "
                             + "WHERE table_schema = 'public' AND table_name = '" + table + "'")) {
            while (rs.next()) {
                columns.add(rs.getString(1));
            }
        }
        return columns;
    }
}

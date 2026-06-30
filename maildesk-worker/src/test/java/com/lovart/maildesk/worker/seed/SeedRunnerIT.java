package com.lovart.maildesk.worker.seed;

import com.lovart.maildesk.worker.MaildeskWorkerApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies SeedRunner loads the dev seed scripts into a fresh Postgres
 * via Testcontainers, and that re-running the seed is a no-op (idempotent).
 *
 * <p>Class-level {@link EnabledIf} gate runs BEFORE SpringExtension so the
 * test is silently skipped on dev laptops without Docker — without the gate
 * the SpringBootTest bootstrap explodes during context init.
 */
@SpringBootTest(
        classes = MaildeskWorkerApplication.class,
        properties = {
                "spring.profiles.active=seed",
                "maildesk.seed.exit-after=false",
                "maildesk.seed.reset=false"
        }
)
@Testcontainers
@EnabledIf("isDockerAvailable")
class SeedRunnerIT {

    static boolean isDockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("maildesk")
            .withUsername("maildesk")
            .withPassword("maildesk_local");

    @DynamicPropertySource
    static void wireDatasource(DynamicPropertyRegistry registry) {
        PG.start();
        registry.add("spring.datasource.url", PG::getJdbcUrl);
        registry.add("spring.datasource.username", PG::getUsername);
        registry.add("spring.datasource.password", PG::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
        registry.add("maildesk.token-encryption-key", () ->
                java.util.Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void seedScripts_populateBusinessTables() {
        Long kolCount = jdbc.queryForObject("SELECT count(*) FROM kols", Long.class);
        Long emailCount = jdbc.queryForObject("SELECT count(*) FROM emails", Long.class);
        Long tplCount = jdbc.queryForObject("SELECT count(*) FROM email_templates", Long.class);

        assertThat(kolCount).as("kols seeded").isPositive();
        assertThat(emailCount).as("emails seeded").isPositive();
        assertThat(tplCount).as("templates seeded").isPositive();
    }
}

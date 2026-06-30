package com.lovart.maildesk.worker.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;

/**
 * Dev seed loader.
 * <p>
 * Activation: {@code spring.profiles.active=seed}. Reads SQL scripts from
 * {@code classpath:/db/seed/} (packaged by {@code maildesk-infrastructure}) and
 * executes them against the configured datasource in a fixed order.
 * <p>
 * Idempotent: every {@code dev-*.sql} uses {@code ON CONFLICT ... DO NOTHING},
 * so re-running the loader on a populated database is a no-op.
 * <p>
 * Optional truncate: pass {@code --maildesk.seed.reset=true} (or set env
 * {@code MAILDESK_SEED_RESET=true}) to TRUNCATE business tables before
 * inserting. Skips the dev tenant row so V3 → seed order stays consistent.
 * <p>
 * After the load completes the worker exits ({@code System.exit(0)}) — the
 * seed profile is not meant to keep the worker process alive.
 */
@Component
@Profile("seed")
public class SeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    /** Load order is significant: parents → children. */
    private static final List<String> SCRIPTS = List.of(
            "db/seed/dev-tenant.sql",
            "db/seed/dev-users.sql",
            "db/seed/dev-templates.sql",
            "db/seed/dev-kols.sql",
            "db/seed/dev-emails.sql"
    );

    private static final String RESET_SCRIPT = "db/seed/dev-reset.sql";

    private final DataSource dataSource;
    private final ConfigurableApplicationContext context;
    private final boolean reset;
    private final boolean exitAfter;

    public SeedRunner(DataSource dataSource,
                      ConfigurableApplicationContext context,
                      Environment env) {
        this.dataSource = dataSource;
        this.context = context;
        this.reset = Boolean.parseBoolean(env.getProperty("maildesk.seed.reset", "false"));
        this.exitAfter = Boolean.parseBoolean(env.getProperty("maildesk.seed.exit-after", "true"));
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("SeedRunner starting — reset={} exitAfter={}", reset, exitAfter);
        try (Connection conn = dataSource.getConnection()) {
            if (reset) {
                runScript(conn, RESET_SCRIPT);
            }
            for (String script : SCRIPTS) {
                runScript(conn, script);
            }
        }
        log.info("SeedRunner finished — {} script(s) executed.",
                SCRIPTS.size() + (reset ? 1 : 0));
        if (exitAfter) {
            int code = SpringApplication.exit(context, () -> 0);
            System.exit(code);
        }
    }

    private void runScript(Connection conn, String path) {
        Resource res = new ClassPathResource(path);
        if (!res.exists()) {
            throw new IllegalStateException("Seed script not found on classpath: " + path);
        }
        log.info("  executing {}", path);
        ScriptUtils.executeSqlScript(
                conn,
                new EncodedResource(res, StandardCharsets.UTF_8),
                /* continueOnError */ false,
                /* ignoreFailedDrops */ false,
                ScriptUtils.DEFAULT_COMMENT_PREFIX,
                ScriptUtils.DEFAULT_STATEMENT_SEPARATOR,
                ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
                ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
    }
}

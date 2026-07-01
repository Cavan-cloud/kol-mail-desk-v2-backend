package com.lovart.maildesk.integration.feishu;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P2-T07 — source-level guard for {@code integration.feishu} (complements ArchUnit in domain).
 */
class FeishuReadOnlySourceTest {

    private static final Path FEISHU_MAIN_SRC =
            Path.of("src/main/java/com/lovart/maildesk/integration/feishu");

    @Test
    void feishuIntegrationSourcesMustNotReferenceWriteApis() throws IOException {
        assertThat(FEISHU_MAIN_SRC).exists();

        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(FEISHU_MAIN_SRC)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> scan(path, violations));
        }
        assertThat(violations)
                .as("Feishu integration must stay read-only — no write API patterns")
                .isEmpty();
    }

    @Test
    void guardDetectsForbiddenWritePatterns() {
        String sample = """
                restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
                """;
        assertThat(FeishuWriteApiGuard.findViolations(sample, "sample"))
                .isNotEmpty();
    }

    @Test
    void guardIgnoresMapPut() {
        String sample = """
                fields.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class));
                """;
        assertThat(FeishuWriteApiGuard.findViolations(sample, "sample"))
                .isEmpty();
    }

    @Test
    void guardAllowsTenantTokenPost() {
        String sample = """
                restTemplate.exchange(
                    apiUrl("/auth/v3/tenant_access_token/internal"),
                    HttpMethod.POST,
                    entity,
                    String.class);
                """;
        assertThat(FeishuWriteApiGuard.findViolations(sample, "sample"))
                .isEmpty();
    }

    private static void scan(Path path, List<String> violations) {
        try {
            String source = Files.readString(path);
            violations.addAll(FeishuWriteApiGuard.findViolations(source, path.toString()));
        } catch (IOException e) {
            violations.add(path + ": unreadable — " + e.getMessage());
        }
    }
}

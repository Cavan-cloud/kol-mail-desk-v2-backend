package com.lovart.maildesk.integration.feishu;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Static analysis helper: flags Feishu write-API patterns in Java source text.
 * Used by {@link FeishuReadOnlySourceTest} to enforce read-only integration.
 */
final class FeishuWriteApiGuard {

    private static final List<Pattern> FORBIDDEN = List.of(
            Pattern.compile("HttpMethod\\.PUT"),
            Pattern.compile("HttpMethod\\.PATCH"),
            Pattern.compile("HttpMethod\\.DELETE"),
            Pattern.compile("restTemplate\\.delete\\("),
            Pattern.compile("restTemplate\\.put\\("),
            Pattern.compile("restTemplate\\.patch\\("),
            Pattern.compile("RestClient.*\\.method\\(\\s*HttpMethod\\.PUT"),
            Pattern.compile("RestClient.*\\.method\\(\\s*HttpMethod\\.PATCH"),
            Pattern.compile("RestClient.*\\.method\\(\\s*HttpMethod\\.DELETE"),
            Pattern.compile("import\\s+com\\.lark\\.oapi"),
            Pattern.compile("import\\s+com\\.larksuite"),
            Pattern.compile("batch_create"),
            Pattern.compile("batch_update"),
            Pattern.compile("batch_delete"),
            Pattern.compile("values_append"),
            Pattern.compile("values_prepend"),
            Pattern.compile("values_batch_update"),
            Pattern.compile("values_batch_clear"),
            Pattern.compile("spreadsheets/.+/sheets/.+/write")
    );

    private FeishuWriteApiGuard() {
    }

    static List<String> findViolations(String source, String label) {
        List<String> violations = new ArrayList<>();
        for (Pattern pattern : FORBIDDEN) {
            if (pattern.matcher(source).find()) {
                violations.add(label + ": forbidden pattern " + pattern.pattern());
            }
        }
        if (containsDisallowedPost(source)) {
            violations.add(label + ": HttpMethod.POST outside tenant_access_token auth endpoint");
        }
        return violations;
    }

    /**
     * POST is allowed only for internal tenant token bootstrap
     * ({@code /auth/v3/tenant_access_token/internal}).
     */
    private static boolean containsDisallowedPost(String source) {
        if (!source.contains("HttpMethod.POST")) {
            return false;
        }
        return !source.contains("tenant_access_token/internal");
    }
}

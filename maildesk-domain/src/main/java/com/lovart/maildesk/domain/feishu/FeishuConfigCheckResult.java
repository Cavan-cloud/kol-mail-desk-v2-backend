package com.lovart.maildesk.domain.feishu;

import java.util.List;

/**
 * Result of a read-only Feishu configuration / connectivity probe.
 */
public record FeishuConfigCheckResult(
        boolean ok,
        String status,
        List<String> missingConfigKeys,
        String spreadsheetTokenPrefix,
        int sheetCount,
        List<String> sheetTitles
) {

    public static FeishuConfigCheckResult notConfigured(List<String> missingKeys) {
        return new FeishuConfigCheckResult(false, "not_configured", missingKeys, null, 0, List.of());
    }

    public static FeishuConfigCheckResult checked(
            boolean ok,
            String spreadsheetTokenPrefix,
            int sheetCount,
            List<String> sheetTitles
    ) {
        return new FeishuConfigCheckResult(
                ok,
                "checked",
                List.of(),
                spreadsheetTokenPrefix,
                sheetCount,
                sheetTitles
        );
    }
}

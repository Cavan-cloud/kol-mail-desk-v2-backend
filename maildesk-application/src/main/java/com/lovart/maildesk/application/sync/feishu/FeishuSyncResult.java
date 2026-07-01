package com.lovart.maildesk.application.sync.feishu;

import java.util.List;

/**
 * Outcome of a Feishu sync run (mirrors legacy {@code syncFeishuKols} return shape).
 */
public record FeishuSyncResult(
        String status,
        String message,
        int sheetsScanned,
        List<SkippedSheet> skippedSheets,
        int scannedRows,
        int mergedPairs,
        int upserted,
        int skipped,
        int ownerMatched,
        boolean dryRun,
        List<SampleRow> samples) {

    public static FeishuSyncResult notConfigured() {
        return new FeishuSyncResult(
                "not_configured",
                "飞书同步环境变量未配置。",
                0,
                List.of(),
                0,
                0,
                0,
                0,
                0,
                false,
                List.of());
    }

    public record SkippedSheet(String title, String reason) {
    }

    public record SampleRow(String email, String operatorName, boolean ownerMatched, String name) {
    }
}

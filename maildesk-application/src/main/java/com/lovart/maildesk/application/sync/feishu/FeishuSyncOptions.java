package com.lovart.maildesk.application.sync.feishu;

import java.util.List;

/**
 * Parameters for a Feishu KOL roster sync run.
 */
public record FeishuSyncOptions(
        String sheetId,
        boolean dryRun,
        Integer maxRecords,
        int recentMonths,
        List<String> noOperatorSheets) {

    public static final int DEFAULT_RECENT_MONTHS = 2;
    public static final List<String> DEFAULT_NO_OPERATOR_SHEETS = List.of("欧美");

    public static FeishuSyncOptions defaults() {
        return new FeishuSyncOptions(null, false, null, DEFAULT_RECENT_MONTHS, DEFAULT_NO_OPERATOR_SHEETS);
    }

    /** Worker delta job — cap merged KOL pairs per run (see {@code FeishuDeltaSyncJob}). */
    public static FeishuSyncOptions deltaBatch(int maxRecords) {
        return new FeishuSyncOptions(null, false, maxRecords, DEFAULT_RECENT_MONTHS, DEFAULT_NO_OPERATOR_SHEETS);
    }

    /** CLI backfill — no row cap; {@code recentMonths=0} scans every sheet tab. */
    public static FeishuSyncOptions backfill(int recentMonths, boolean dryRun) {
        return new FeishuSyncOptions(null, dryRun, null, recentMonths, DEFAULT_NO_OPERATOR_SHEETS);
    }

    public FeishuSyncOptions {
        if (recentMonths < 0) {
            recentMonths = DEFAULT_RECENT_MONTHS;
        }
        if (noOperatorSheets == null || noOperatorSheets.isEmpty()) {
            noOperatorSheets = DEFAULT_NO_OPERATOR_SHEETS;
        }
    }
}

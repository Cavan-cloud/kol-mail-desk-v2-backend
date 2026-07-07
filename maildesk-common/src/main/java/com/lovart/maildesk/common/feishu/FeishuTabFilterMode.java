package com.lovart.maildesk.common.feishu;

/**
 * Which spreadsheet tabs / Bitable tables participate in a Feishu sync run.
 */
public enum FeishuTabFilterMode {

    /** All {@code N月} tabs plus configured regional rosters (欧美/拉美/韩国). */
    ROSTER,
    /** Legacy rolling window: recent month tabs + any tab without a month in the title. */
    RECENT_MONTHS,
    /** Every tab/table in the workbook/base. */
    ALL;

    public static FeishuTabFilterMode fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return ROSTER;
        }
        return switch (value.trim().toLowerCase()) {
            case "recent-months", "recent" -> RECENT_MONTHS;
            case "all" -> ALL;
            default -> ROSTER;
        };
    }
}

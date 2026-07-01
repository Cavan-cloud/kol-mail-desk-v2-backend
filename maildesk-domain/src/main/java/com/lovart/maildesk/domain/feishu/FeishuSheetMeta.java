package com.lovart.maildesk.domain.feishu;

/**
 * Metadata for one tab inside a Feishu spreadsheet workbook.
 */
public record FeishuSheetMeta(
        String sheetId,
        String title,
        int rowCount,
        int columnCount
) {
}

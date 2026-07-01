package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.common.feishu.FeishuCellExtractor;

import java.util.List;

/**
 * Resolves Feishu sheet header row labels to column indices.
 */
public final class FeishuColumnResolver {

    private FeishuColumnResolver() {
    }

    public static FeishuColumnIndex resolve(List<String> headerRow, FeishuFieldHeaders headers) {
        return new FeishuColumnIndex(
                findColumn(headerRow, headers.email()),
                findColumn(headerRow, headers.operator()),
                findColumn(headerRow, headers.name()),
                findColumn(headerRow, headers.profileUrl()),
                findColumn(headerRow, headers.platform()),
                findColumn(headerRow, headers.country()),
                findColumn(headerRow, headers.language()),
                findColumn(headerRow, headers.type()),
                findColumn(headerRow, headers.followers()),
                findColumn(headerRow, headers.price()),
                findColumn(headerRow, headers.cooperation()),
                findColumn(headerRow, headers.finalCooperation()),
                findColumn(headerRow, headers.stage()),
                findColumn(headerRow, headers.outreachDate()),
                findColumn(headerRow, headers.notes()));
    }

    /**
     * Returns trimmed header labels from the first row of sheet values.
     */
    public static List<String> normalizeHeaderRow(List<?> headerRow) {
        if (headerRow == null) {
            return List.of();
        }
        return headerRow.stream()
                .map(FeishuCellExtractor::extractText)
                .map(String::trim)
                .toList();
    }

    static Integer findColumn(List<String> headerRow, List<String> candidates) {
        for (String candidate : candidates) {
            for (int i = 0; i < headerRow.size(); i++) {
                if (candidate.equals(headerRow.get(i))) {
                    return i;
                }
            }
        }
        for (String candidate : candidates) {
            for (int i = 0; i < headerRow.size(); i++) {
                if (headerRow.get(i).contains(candidate)) {
                    return i;
                }
            }
        }
        return null;
    }
}

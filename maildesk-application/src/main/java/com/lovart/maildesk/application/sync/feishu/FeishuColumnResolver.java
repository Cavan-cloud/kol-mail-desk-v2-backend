package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.common.feishu.FeishuCellExtractor;

import java.util.List;
import java.util.Objects;

/**
 * Resolves Feishu sheet header row labels to column indices.
 */
public final class FeishuColumnResolver {

    private static final List<String> KOL_QUOTE_HEADERS = List.of("KOL报价($)", "KOL报价（$）");

    private FeishuColumnResolver() {
    }

    public static FeishuColumnIndex resolve(List<String> headerRow, FeishuFieldHeaders headers) {
        Integer brandQuote = findColumn(headerRow, headers.brandQuote(), List.of("最终"));
        Integer kolQuote = findColumn(headerRow, KOL_QUOTE_HEADERS, List.of());
        if (kolQuote != null && Objects.equals(kolQuote, brandQuote)) {
            kolQuote = null;
        }
        return new FeishuColumnIndex(
                findColumn(headerRow, headers.email(), List.of()),
                findColumn(headerRow, headers.operator(), List.of()),
                findColumn(headerRow, headers.name(), List.of()),
                findColumn(headerRow, headers.profileUrl(), List.of()),
                findColumn(headerRow, headers.platform(), List.of()),
                findColumn(headerRow, headers.country(), List.of()),
                findColumn(headerRow, headers.language(), List.of()),
                findColumn(headerRow, headers.type(), List.of()),
                findColumn(headerRow, headers.followers(), List.of()),
                brandQuote,
                kolQuote,
                findColumn(headerRow, headers.finalCooperationPrice(), List.of()),
                findColumn(headerRow, headers.cooperation(), List.of()),
                findColumn(headerRow, headers.finalCooperation(), List.of()),
                findColumn(headerRow, headers.stage(), List.of()),
                findColumn(headerRow, headers.outreachDate(), List.of()),
                findColumn(headerRow, headers.notes(), List.of()));
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

    /**
     * @param excludeContains skip headers whose text contains any of these tokens (contains-phase only)
     */
    static Integer findColumn(List<String> headerRow, List<String> candidates, List<String> excludeContains) {
        if (headerRow == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        for (String candidate : candidates) {
            for (int i = 0; i < headerRow.size(); i++) {
                if (candidate.equals(headerRow.get(i))) {
                    return i;
                }
            }
        }
        for (String candidate : candidates) {
            for (int i = 0; i < headerRow.size(); i++) {
                String header = headerRow.get(i);
                if (header == null || !header.contains(candidate)) {
                    continue;
                }
                if (isExcluded(header, excludeContains)) {
                    continue;
                }
                return i;
            }
        }
        return null;
    }

    private static boolean isExcluded(String header, List<String> excludeContains) {
        if (excludeContains == null || excludeContains.isEmpty()) {
            return false;
        }
        for (String token : excludeContains) {
            if (token != null && !token.isBlank() && header.contains(token)) {
                return true;
            }
        }
        return false;
    }
}

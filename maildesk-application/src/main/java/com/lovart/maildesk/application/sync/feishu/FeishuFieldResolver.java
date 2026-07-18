package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.common.feishu.FeishuCellExtractor;

import java.util.List;
import java.util.Map;

/**
 * Resolves logical field values from Feishu Bitable {@code fields} maps by header label candidates.
 */
public final class FeishuFieldResolver {

    private FeishuFieldResolver() {
    }

    public static String pickField(Map<String, Object> fields, List<String> candidates) {
        return pickFirstNonBlank(fields, candidates, List.of());
    }

    /**
     * Picks the first non-blank candidate value (exact key, then contains), skipping empty cells
     * so later fallbacks like {@code KOL报价($)} can win when {@code 品牌报价} is blank.
     *
     * @param excludeContains skip field keys containing these tokens during the contains phase
     */
    public static String pickFirstNonBlank(
            Map<String, Object> fields, List<String> candidates, List<String> excludeContains) {
        if (fields == null || fields.isEmpty() || candidates == null || candidates.isEmpty()) {
            return "";
        }
        for (String candidate : candidates) {
            Object value = fields.get(candidate);
            if (value != null) {
                String text = FeishuCellExtractor.extractText(value);
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        for (String candidate : candidates) {
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                String key = entry.getKey();
                if (key == null || !key.contains(candidate) || isExcluded(key, excludeContains)) {
                    continue;
                }
                String text = FeishuCellExtractor.extractText(entry.getValue());
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

    private static boolean isExcluded(String key, List<String> excludeContains) {
        if (excludeContains == null || excludeContains.isEmpty()) {
            return false;
        }
        for (String token : excludeContains) {
            if (token != null && !token.isBlank() && key.contains(token)) {
                return true;
            }
        }
        return false;
    }

    public static Object pickFieldRaw(Map<String, Object> fields, List<String> candidates) {
        if (fields == null || fields.isEmpty() || candidates == null || candidates.isEmpty()) {
            return null;
        }
        for (String candidate : candidates) {
            Object value = fields.get(candidate);
            if (value != null) {
                return value;
            }
        }
        for (String candidate : candidates) {
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                if (entry.getKey() != null && entry.getKey().contains(candidate)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }
}

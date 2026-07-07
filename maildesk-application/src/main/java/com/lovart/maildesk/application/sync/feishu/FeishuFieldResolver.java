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
        if (fields == null || fields.isEmpty() || candidates == null || candidates.isEmpty()) {
            return "";
        }
        for (String candidate : candidates) {
            Object value = fields.get(candidate);
            if (value != null) {
                return FeishuCellExtractor.extractText(value);
            }
        }
        for (String candidate : candidates) {
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                if (entry.getKey() != null && entry.getKey().contains(candidate)) {
                    return FeishuCellExtractor.extractText(entry.getValue());
                }
            }
        }
        return "";
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

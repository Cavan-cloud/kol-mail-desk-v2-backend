package com.lovart.maildesk.common.feishu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts plain text, emails, and URLs from Feishu Sheet cell values.
 * Ported from legacy {@code lib/feishu/sync-kols.ts}.
 */
public final class FeishuCellExtractor {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s,，]+", Pattern.CASE_INSENSITIVE);

    private FeishuCellExtractor() {
    }

    public static String extractText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder joined = new StringBuilder();
            for (Object item : list) {
                String part = extractText(item);
                if (!part.isEmpty()) {
                    if (!joined.isEmpty()) {
                        joined.append(", ");
                    }
                    joined.append(part);
                }
            }
            return joined.toString();
        }
        if (value instanceof java.util.Map<?, ?> map) {
            for (String key : List.of("text", "name", "link", "url", "email", "value", "en_name", "full_name")) {
                Object nested = map.get(key);
                if (nested != null) {
                    return extractText(nested);
                }
            }
            StringBuilder joined = new StringBuilder();
            for (Object nested : map.values()) {
                String part = extractText(nested);
                if (!part.isEmpty()) {
                    if (!joined.isEmpty()) {
                        joined.append(", ");
                    }
                    joined.append(part);
                }
            }
            return joined.toString();
        }
        return value.toString();
    }

    public static String extractEmail(Object value) {
        String text = extractText(value).replaceFirst("(?i)^mailto:", "");
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        return matcher.find() ? matcher.group() : "";
    }

    public static String extractUrl(Object value) {
        String text = extractText(value);
        Matcher matcher = URL_PATTERN.matcher(text);
        return matcher.find() ? matcher.group() : text;
    }

    /**
     * Operator cells may render as merged {@code @甲@乙} mentions; split on common separators.
     */
    public static List<String> extractOperatorNames(Object value) {
        List<String> names = new ArrayList<>();
        for (String text : extractTextList(value)) {
            for (String part : text.split("[,，、/|@]")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    names.add(trimmed);
                }
            }
        }
        return names;
    }

    public static String normalizeOperatorName(Object value) {
        return extractText(value)
                .trim()
                .replaceFirst("^@+", "")
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Merge key for deduplicating sheet rows: {@code email|normalizedOperator}.
     */
    public static String mergeKey(String email, String operatorName) {
        String normalizedEmail = email == null ? "" : email.toLowerCase(Locale.ROOT).trim();
        return normalizedEmail + "|" + normalizeOperatorName(operatorName);
    }

    private static List<String> extractTextList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
            return List.of(value.toString());
        }
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object item : list) {
                out.addAll(extractTextList(item));
            }
            return out;
        }
        if (value instanceof java.util.Map<?, ?> map) {
            for (String key : List.of("en_name", "name", "text", "full_name", "email", "value")) {
                Object nested = map.get(key);
                if (nested != null) {
                    return extractTextList(nested);
                }
            }
            List<String> out = new ArrayList<>();
            for (Object nested : map.values()) {
                out.addAll(extractTextList(nested));
            }
            return out;
        }
        return List.of(value.toString());
    }
}

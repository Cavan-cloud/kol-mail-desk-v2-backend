package com.lovart.maildesk.application.sync.feishu;

import java.util.Locale;

/**
 * Normalizes Feishu platform labels to lowercase slugs stored in {@code kols.primary_platform}.
 */
public final class FeishuPlatformNormalizer {

    private FeishuPlatformNormalizer() {
    }

    public static String normalizePlatform(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("tiktok")) {
            return "tiktok";
        }
        if (normalized.contains("instagram") || normalized.contains("ins")) {
            return "instagram";
        }
        if (normalized.contains("youtube")) {
            return "youtube";
        }
        if ("x".equals(normalized) || normalized.contains("twitter")) {
            return "x";
        }
        return null;
    }

    public static String normalizePlatformFromUrl(String url) {
        return url == null ? null : normalizePlatform(url);
    }
}

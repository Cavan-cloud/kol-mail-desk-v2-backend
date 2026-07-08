package com.lovart.maildesk.application.support;

import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.domain.kol.entity.KolDO;

import java.util.Locale;

/**
 * Board drill-down filters (v1 {@code getDetailItems}).
 */
public final class BoardDetailFilter {

    private BoardDetailFilter() {
    }

    public static String normalizeDetail(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "kols", "unreplied", "unread" -> raw.trim().toLowerCase(Locale.ROOT);
            default -> null;
        };
    }

    public static KolStage parseStage(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return KolStage.fromApiValue(raw.trim());
    }

    public static boolean matchesDetail(KolDO kol, String detail, int unreadCount, boolean unreplied) {
        if (detail == null) {
            return false;
        }
        return switch (detail) {
            case "unread" -> unreadCount > 0;
            case "unreplied" -> unreplied;
            case "kols" -> true;
            default -> false;
        };
    }
}

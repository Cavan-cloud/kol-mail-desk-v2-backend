package com.lovart.maildesk.application.support;

import com.lovart.maildesk.domain.email.entity.EmailDO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Collapses duplicate rows that appear when several operators sync the same Gmail message.
 */
public final class EmailDedupe {

    private EmailDedupe() {
    }

    /**
     * Keeps one row per {@code gmail_message_id} (falls back to row id). Prefers the viewer's copy
     * when both exist so personal read-state stays authoritative for that user.
     */
    public static List<EmailDO> dedupeForViewer(List<EmailDO> rows, UUID viewerUserId) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<String, EmailDO> byKey = new LinkedHashMap<>();
        for (EmailDO email : rows) {
            if (email == null) {
                continue;
            }
            String key = dedupeKey(email);
            EmailDO existing = byKey.get(key);
            if (existing == null) {
                byKey.put(key, email);
                continue;
            }
            if (viewerUserId != null
                    && viewerUserId.equals(email.getUserId())
                    && !viewerUserId.equals(existing.getUserId())) {
                byKey.put(key, email);
            }
        }
        List<EmailDO> deduped = new ArrayList<>(byKey.values());
        deduped.sort(Comparator.comparing(
                (EmailDO e) -> e.getSentAt() == null ? 0L : e.getSentAt().toInstant().toEpochMilli()
        ).reversed());
        return deduped;
    }

    private static String dedupeKey(EmailDO email) {
        String messageId = email.getGmailMessageId();
        if (messageId != null && !messageId.isBlank()) {
            return "msg:" + messageId;
        }
        return "id:" + (email.getId() == null ? System.identityHashCode(email) : email.getId());
    }
}

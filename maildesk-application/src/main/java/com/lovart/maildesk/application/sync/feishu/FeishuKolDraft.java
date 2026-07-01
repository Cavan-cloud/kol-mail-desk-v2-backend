package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.common.enums.KolStage;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Parsed KOL fields from one Feishu sheet row, ready for upsert in {@code FeishuSyncService}.
 */
public record FeishuKolDraft(
        String email,
        String operatorName,
        String name,
        String profileUrl,
        String primaryPlatform,
        String handle,
        String type,
        BigDecimal agreedPrice,
        KolStage stage,
        LocalDate feishuOutreachAt,
        String notes) {

    public String displayName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}

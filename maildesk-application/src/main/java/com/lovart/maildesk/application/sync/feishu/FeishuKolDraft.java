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
        String brandQuote,
        BigDecimal finalCooperationPrice,
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

    /**
     * Best numeric price for templates: final cooperation price first, else parsed brand quote.
     */
    public BigDecimal agreedPrice() {
        if (finalCooperationPrice != null) {
            return finalCooperationPrice;
        }
        return FeishuRowMapper.parsePrice(brandQuote);
    }

    /**
     * Prefer the draft that carries more price information when the same email|operator
     * appears across multiple Feishu tabs (month sheets are scanned before regional tabs).
     */
    public static FeishuKolDraft preferRicherPrices(FeishuKolDraft current, FeishuKolDraft incoming) {
        if (current == null) {
            return incoming;
        }
        if (incoming == null) {
            return current;
        }
        int currentScore = priceScore(current);
        int incomingScore = priceScore(incoming);
        if (incomingScore > currentScore) {
            return incoming;
        }
        return current;
    }

    private static int priceScore(FeishuKolDraft draft) {
        int score = 0;
        if (draft.brandQuote() != null && !draft.brandQuote().isBlank()) {
            score += 1;
        }
        if (draft.finalCooperationPrice() != null) {
            score += 2;
        }
        return score;
    }
}

package com.lovart.maildesk.ai.translate;

/**
 * Translation context (mirrors legacy {@code mode} in {@code lib/ai/client.ts#translateText}).
 */
public enum TranslateMode {
    EMAIL_BODY,
    SEND_DRAFT;

    public String apiValue() {
        return name().toLowerCase();
    }
}

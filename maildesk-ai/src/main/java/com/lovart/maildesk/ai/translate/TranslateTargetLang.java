package com.lovart.maildesk.ai.translate;

/**
 * Target language for {@link com.lovart.maildesk.ai.AiService#translateText(TranslateTextRequest)}.
 */
public enum TranslateTargetLang {
    ZH,
    EN,
    KO;

    public String apiValue() {
        return name().toLowerCase();
    }

    public static TranslateTargetLang fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            return ZH;
        }
        return switch (value.trim().toLowerCase()) {
            case "en" -> EN;
            case "ko" -> KO;
            default -> ZH;
        };
    }

    /** Outbound send-draft translations (Chinese editor → final confirm draft). */
    public boolean isSendDraftTarget() {
        return this == EN || this == KO;
    }
}

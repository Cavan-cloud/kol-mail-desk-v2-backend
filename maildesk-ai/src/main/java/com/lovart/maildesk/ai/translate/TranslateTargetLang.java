package com.lovart.maildesk.ai.translate;

/**
 * Target language for {@link com.lovart.maildesk.ai.AiService#translateText(TranslateTextRequest)}.
 */
public enum TranslateTargetLang {
    ZH,
    EN;

    public String apiValue() {
        return name().toLowerCase();
    }

    public static TranslateTargetLang fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            return ZH;
        }
        return value.equalsIgnoreCase("en") ? EN : ZH;
    }
}

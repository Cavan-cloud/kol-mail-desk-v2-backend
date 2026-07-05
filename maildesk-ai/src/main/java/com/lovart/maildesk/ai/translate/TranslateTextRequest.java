package com.lovart.maildesk.ai.translate;

/**
 * Input for {@link com.lovart.maildesk.ai.AiService#translateText(TranslateTextRequest)}.
 */
public record TranslateTextRequest(String text, TranslateTargetLang targetLang, TranslateMode mode) {

    public TranslateTextRequest {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        if (targetLang == null) {
            targetLang = TranslateTargetLang.ZH;
        }
        if (mode == null) {
            mode = TranslateMode.EMAIL_BODY;
        }
    }

    public TranslateTextRequest(String text, TranslateTargetLang targetLang) {
        this(text, targetLang, TranslateMode.EMAIL_BODY);
    }
}

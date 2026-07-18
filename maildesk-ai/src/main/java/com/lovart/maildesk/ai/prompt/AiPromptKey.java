package com.lovart.maildesk.ai.prompt;

/**
 * Classpath prompt templates under {@code resources/prompts/*.st}.
 * Maps 1:1 to legacy {@code lib/ai/prompts.ts} exports (classify schema updated per cost design).
 */
public enum AiPromptKey {

    CLASSIFY_EMAIL("prompts/classify-email.st"),
    REPLY_DRAFT("prompts/reply-draft.st"),
    CHECK_DRAFT("prompts/check-draft.st"),
    TRANSLATE_ZH_TO_EN("prompts/translate-zh-to-en.st"),
    TRANSLATE_ZH_TO_KO("prompts/translate-zh-to-ko.st"),
    TRANSLATE_EMAIL_TO_ZH("prompts/translate-email-to-zh.st");

    private final String classpathLocation;

    AiPromptKey(String classpathLocation) {
        this.classpathLocation = classpathLocation;
    }

    public String classpathLocation() {
        return classpathLocation;
    }
}

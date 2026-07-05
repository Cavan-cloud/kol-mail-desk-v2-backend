package com.lovart.maildesk.ai.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptCatalogTest {

    private final AiPromptCatalog catalog = new AiPromptCatalog();

    @Test
    void loadsAllLegacyPrompts() {
        for (AiPromptKey key : AiPromptKey.values()) {
            assertThat(catalog.systemPrompt(key)).isNotBlank();
        }
    }

    @Test
    void classifyPromptOmitsBodyZhPerCostDesign() {
        String prompt = catalog.systemPrompt(AiPromptKey.CLASSIFY_EMAIL);

        assertThat(prompt).doesNotContain("body_zh");
        assertThat(prompt).contains("stage_signal");
        assertThat(prompt).contains("priority");
        assertThat(prompt).contains("summary");
        assertThat(prompt).contains("Do NOT translate the full email body");
    }

    @Test
    void replyDraftPromptMatchesLegacyJsonSchema() {
        String prompt = catalog.systemPrompt(AiPromptKey.REPLY_DRAFT);

        assertThat(prompt).contains("AI 生成回复");
        assertThat(prompt).contains("\"english\"");
        assertThat(prompt).contains("\"chinese\"");
    }

    @Test
    void checkDraftPromptRequiresIssuesArray() {
        String prompt = catalog.systemPrompt(AiPromptKey.CHECK_DRAFT);

        assertThat(prompt).contains("\"issues\"");
        assertThat(prompt).contains("⚠️");
    }

    @Test
    void translatePromptsMatchLegacyIntent() {
        assertThat(catalog.systemPrompt(AiPromptKey.TRANSLATE_ZH_TO_EN))
                .contains("native-level English");
        assertThat(catalog.systemPrompt(AiPromptKey.TRANSLATE_EMAIL_TO_ZH))
                .contains("Do not summarize");
    }
}

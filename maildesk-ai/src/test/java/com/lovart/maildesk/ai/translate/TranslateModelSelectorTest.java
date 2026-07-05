package com.lovart.maildesk.ai.translate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TranslateModelSelectorTest {

    @Test
    void keeps8kModelForShortText() {
        assertThat(TranslateModelSelector.selectModel("moonshot-v1-8k", 1000))
                .isEqualTo("moonshot-v1-8k");
    }

    @Test
    void upgrades8kTo32kForLongText() {
        assertThat(TranslateModelSelector.selectModel("moonshot-v1-8k", 6000))
                .isEqualTo("moonshot-v1-32k");
    }

    @Test
    void upgradesDeepSeekFlashToProForLongText() {
        assertThat(TranslateModelSelector.selectModel("deepseek-v4-flash", 6000))
                .isEqualTo("deepseek-v4-pro");
    }

    @Test
    void keeps128kModelUnchanged() {
        assertThat(TranslateModelSelector.selectModel("moonshot-v1-128k", 20_000))
                .isEqualTo("moonshot-v1-128k");
    }
}

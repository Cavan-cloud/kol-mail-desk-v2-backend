package com.lovart.maildesk.ai.translate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TranslateTextRequestTest {

    @Test
    void defaultsTargetLangAndMode() {
        TranslateTextRequest request = new TranslateTextRequest("hello", null, null);

        assertThat(request.targetLang()).isEqualTo(TranslateTargetLang.ZH);
        assertThat(request.mode()).isEqualTo(TranslateMode.EMAIL_BODY);
    }

    @Test
    void rejectsBlankText() {
        assertThatThrownBy(() -> new TranslateTextRequest("  ", TranslateTargetLang.ZH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parsesTargetLangFromApiValue() {
        assertThat(TranslateTargetLang.fromApiValue("en")).isEqualTo(TranslateTargetLang.EN);
        assertThat(TranslateTargetLang.fromApiValue(null)).isEqualTo(TranslateTargetLang.ZH);
    }
}

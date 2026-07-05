package com.lovart.maildesk.ai.draft;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReplyDraftParserTest {

    private final ReplyDraftParser parser = new ReplyDraftParser(new ObjectMapper());

    @Test
    void parsesStrictJson() {
        ReplyDraftResult result = parser.parse(
                """
                {
                  "english": "Hi Alice, thanks.",
                  "chinese": "Hi Alice，谢谢。"
                }
                """);

        assertThat(result.english()).isEqualTo("Hi Alice, thanks.");
        assertThat(result.chinese()).isEqualTo("Hi Alice，谢谢。");
        assertThat(result.fallback()).isFalse();
    }

    @Test
    void extractsJsonEmbeddedInProse() {
        ReplyDraftResult result = parser.parse(
                "Draft:\n{\"english\":\"Hello\",\"chinese\":\"你好\"}\n");

        assertThat(result.english()).isEqualTo("Hello");
        assertThat(result.chinese()).isEqualTo("你好");
    }

    @Test
    void rejectsMissingEnglish() {
        assertThatThrownBy(() -> parser.parse("{\"chinese\":\"你好\"}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

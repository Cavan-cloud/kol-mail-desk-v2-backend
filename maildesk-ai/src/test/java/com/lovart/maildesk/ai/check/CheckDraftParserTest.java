package com.lovart.maildesk.ai.check;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckDraftParserTest {

    private final CheckDraftParser parser = new CheckDraftParser(new ObjectMapper());

    @Test
    void parsesIssuesArray() {
        CheckDraftResult result = parser.parse(
                """
                {
                  "issues": ["⚠️ 未提及截止日期。", "⚠️ 未说明交付物。"]
                }
                """);

        assertThat(result.issues()).containsExactly("⚠️ 未提及截止日期。", "⚠️ 未说明交付物。");
        assertThat(result.fallback()).isFalse();
    }

    @Test
    void acceptsEmptyIssuesArray() {
        CheckDraftResult result = parser.parse("{\"issues\":[]}");

        assertThat(result.issues()).isEmpty();
    }

    @Test
    void rejectsMissingIssuesArray() {
        assertThatThrownBy(() -> parser.parse("{\"warnings\":[]}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

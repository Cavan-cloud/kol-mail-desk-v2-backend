package com.lovart.maildesk.ai.classify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.common.enums.KolStage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailClassificationParserTest {

    private final EmailClassificationParser parser = new EmailClassificationParser(new ObjectMapper());

    @Test
    void parsesStrictJsonWithoutBodyZh() {
        String json =
                """
                {
                  "stage_signal": "negotiating",
                  "priority": "high",
                  "summary": "达人报价讨论",
                  "extracted": {
                    "price_usd": 500,
                    "platform": "youtube",
                    "deadline": null,
                    "deliverables": "1 video"
                  },
                  "suggested_action": "确认报价"
                }
                """;

        EmailClassificationResult result = parser.parse(json);

        assertThat(result.stageSignal()).isEqualTo(KolStage.NEGOTIATING);
        assertThat(result.priority()).isEqualTo("high");
        assertThat(result.summary()).isEqualTo("达人报价讨论");
        assertThat(result.suggestedAction()).isEqualTo("确认报价");
        assertThat(result.extractedFields().path("price_usd").asInt()).isEqualTo(500);
        assertThat(result.fallback()).isFalse();
    }

    @Test
    void extractsJsonEmbeddedInProse() {
        EmailClassificationResult result = parser.parse(
                "Here is the result:\n{\"stage_signal\":\"replied\",\"priority\":\"medium\","
                        + "\"summary\":\"已回复\",\"extracted\":{},\"suggested_action\":\"跟进\"}\n");

        assertThat(result.stageSignal()).isEqualTo(KolStage.REPLIED);
        assertThat(result.priority()).isEqualTo("medium");
    }

    @Test
    void rejectsInvalidPriority() {
        assertThatThrownBy(() -> parser.parse(
                        "{\"stage_signal\":\"replied\",\"priority\":\"urgent\",\"summary\":\"x\","
                                + "\"extracted\":{},\"suggested_action\":\"y\"}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

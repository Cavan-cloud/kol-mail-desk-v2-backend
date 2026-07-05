package com.lovart.maildesk.common.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KolStageJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializesLowercaseStageFromJson() throws Exception {
        KolStage stage = mapper.readValue("\"negotiating\"", KolStage.class);

        assertThat(stage).isEqualTo(KolStage.NEGOTIATING);
    }

    @Test
    void serializesLowercaseStageForApiResponse() throws Exception {
        String json = mapper.writeValueAsString(KolStage.REPLIED);

        assertThat(json).isEqualTo("\"replied\"");
    }
}

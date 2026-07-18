package com.lovart.maildesk.ai.config;

import com.lovart.maildesk.ai.AiResolvedTarget;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DeepSeekChatOptionsTest {

    private final ChatModel chatModel = mock(ChatModel.class);

    @Test
    void addsThinkingDisabledExtraBodyForDeepSeekProvider() {
        AiResolvedTarget target = new AiResolvedTarget("deepseek", "deepseek-v4-flash", chatModel, false);

        OpenAiChatOptions options = DeepSeekChatOptions.builder(target).temperature(0.1).build();

        assertThat(options.getExtraBody())
                .containsEntry("thinking", java.util.Map.of("type", "disabled"));
    }

    @Test
    void skipsExtraBodyForNonDeepSeekProvider() {
        AiResolvedTarget target = new AiResolvedTarget("moonshot", "moonshot-v1-8k", chatModel, false);

        OpenAiChatOptions options = DeepSeekChatOptions.builder(target).temperature(0.1).build();

        assertThat(options.getExtraBody()).isNullOrEmpty();
    }
}

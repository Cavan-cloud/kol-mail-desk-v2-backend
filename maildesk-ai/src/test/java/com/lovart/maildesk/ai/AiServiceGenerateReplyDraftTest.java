package com.lovart.maildesk.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.ai.check.CheckDraftParser;
import com.lovart.maildesk.ai.classify.EmailClassificationParser;
import com.lovart.maildesk.ai.fallback.AiInvocationPipeline;
import com.lovart.maildesk.ai.draft.ReplyDraftParser;
import com.lovart.maildesk.ai.draft.ReplyDraftRequest;
import com.lovart.maildesk.ai.draft.ReplyDraftResult;
import com.lovart.maildesk.ai.config.AiProviderProperties;
import com.lovart.maildesk.ai.prompt.AiPromptCatalog;
import com.lovart.maildesk.common.enums.KolStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceGenerateReplyDraftTest {

    @Mock
    private ChatModel moonshotModel;

    @Mock
    private ChatModel deepseekModel;

    private AiService aiService;

    @BeforeEach
    void setUp() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setDefaultProvider("moonshot");

        AiProviderProperties.Provider moonshot = new AiProviderProperties.Provider();
        moonshot.setBaseUrl("https://api.moonshot.cn/v1");
        moonshot.setApiKey("moonshot-key");
        AiProviderProperties.Provider deepseek = new AiProviderProperties.Provider();
        deepseek.setBaseUrl("https://api.deepseek.com");
        deepseek.setApiKey("deepseek-key");
        properties.setProviders(Map.of("moonshot", moonshot, "deepseek", deepseek));

        AiProviderProperties.CapabilityBinding draft = new AiProviderProperties.CapabilityBinding();
        draft.setProvider("moonshot");
        draft.setModel("moonshot-v1-128k");
        draft.setFallbackProvider("deepseek");
        draft.setFallbackModel("deepseek-v4-pro");
        properties.setCapabilities(Map.of("draft", draft));

        ObjectMapper objectMapper = new ObjectMapper();
        AiModelRouter router = new AiModelRouter(
                properties, Map.of("moonshot", moonshotModel, "deepseek", deepseekModel));
        aiService = new AiService(
                new AiInvocationPipeline(router),
                new AiPromptCatalog(),
                new EmailClassificationParser(objectMapper),
                new ReplyDraftParser(objectMapper),
                new CheckDraftParser(objectMapper),
                objectMapper);
    }

    @Test
    void generateReplyDraftUsesTemplateWhenNoProviderConfigured() {
        AiProviderProperties emptyProps = new AiProviderProperties();
        emptyProps.setDefaultProvider("moonshot");
        ObjectMapper objectMapper = new ObjectMapper();
        AiService service = new AiService(
                new AiInvocationPipeline(new AiModelRouter(emptyProps, Map.of())),
                new AiPromptCatalog(),
                new EmailClassificationParser(objectMapper),
                new ReplyDraftParser(objectMapper),
                new CheckDraftParser(objectMapper),
                objectMapper);

        ReplyDraftResult result = service.generateReplyDraft(sampleRequest());

        assertThat(result.fallback()).isTrue();
        assertThat(result.english()).contains("Alice");
        assertThat(result.chinese()).contains("Alice");
        assertThat(result.aiError()).isNull();
    }

    @Test
    void generateReplyDraftReturnsParsedLlmJson() {
        when(moonshotModel.call(any(Prompt.class))).thenReturn(llmResponse(validDraftJson()));

        ReplyDraftResult result = aiService.generateReplyDraft(sampleRequest());

        assertThat(result.fallback()).isFalse();
        assertThat(result.english()).isEqualTo("Hi Alice, thanks for reaching out.");
        assertThat(result.chinese()).isEqualTo("Hi Alice，感谢联系。");
    }

    @Test
    void generateReplyDraftFallsBackToSecondaryProvider() {
        when(moonshotModel.call(any(Prompt.class))).thenThrow(new RuntimeException("429 rate limit"));
        when(deepseekModel.call(any(Prompt.class))).thenReturn(llmResponse(validDraftJson()));

        ReplyDraftResult result = aiService.generateReplyDraft(sampleRequest());

        assertThat(result.fallback()).isFalse();
        assertThat(result.english()).contains("Alice");
        verify(deepseekModel).call(any(Prompt.class));
    }

    @Test
    void generateReplyDraftUsesTemplateWhenAllProvidersFail() {
        when(moonshotModel.call(any(Prompt.class))).thenThrow(new RuntimeException("timeout"));
        when(deepseekModel.call(any(Prompt.class))).thenThrow(new RuntimeException("401"));

        ReplyDraftResult result = aiService.generateReplyDraft(sampleRequest());

        assertThat(result.fallback()).isTrue();
        assertThat(result.english()).contains("Thanks for your message");
        assertThat(result.aiError()).isEqualTo("AI 草稿生成失败");
    }

    @Test
    void generateReplyDraftDeclinedKolUsesManualMessage() {
        AiProviderProperties emptyProps = new AiProviderProperties();
        emptyProps.setDefaultProvider("moonshot");
        ObjectMapper objectMapper = new ObjectMapper();
        AiService service = new AiService(
                new AiInvocationPipeline(new AiModelRouter(emptyProps, Map.of())),
                new AiPromptCatalog(),
                new EmailClassificationParser(objectMapper),
                new ReplyDraftParser(objectMapper),
                new CheckDraftParser(objectMapper),
                objectMapper);

        ReplyDraftRequest declined = new ReplyDraftRequest(
                "Alice", "Bob", "No thanks", "", null, KolStage.DECLINED, "youtube", null);

        ReplyDraftResult result = service.generateReplyDraft(declined);

        assertThat(result.fallback()).isTrue();
        assertThat(result.english()).contains("declined");
        assertThat(result.chinese()).contains("拒绝合作");
    }

    private static ReplyDraftRequest sampleRequest() {
        return new ReplyDraftRequest(
                "Alice",
                "Bob",
                "What is your rate?",
                "Previous thread about collaboration.",
                "Mention timeline",
                KolStage.NEGOTIATING,
                "youtube",
                500.0);
    }

    private static String validDraftJson() {
        return """
                {
                  "english": "Hi Alice, thanks for reaching out.",
                  "chinese": "Hi Alice，感谢联系。"
                }
                """;
    }

    private static ChatResponse llmResponse(String content) {
        return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(content))))
                .build();
    }
}

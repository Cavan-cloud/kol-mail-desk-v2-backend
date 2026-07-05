package com.lovart.maildesk.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.ai.check.CheckDraftParser;
import com.lovart.maildesk.ai.check.CheckDraftRequest;
import com.lovart.maildesk.ai.check.CheckDraftResult;
import com.lovart.maildesk.ai.classify.EmailClassificationParser;
import com.lovart.maildesk.ai.fallback.AiInvocationPipeline;
import com.lovart.maildesk.ai.draft.ReplyDraftParser;
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
class AiServiceCheckDraftTest {

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

        AiProviderProperties.CapabilityBinding check = new AiProviderProperties.CapabilityBinding();
        check.setProvider("moonshot");
        check.setModel("moonshot-v1-8k");
        check.setFallbackProvider("deepseek");
        check.setFallbackModel("deepseek-v4-flash");
        properties.setCapabilities(Map.of("check", check));

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
    void checkDraftReturnsEmptyIssuesWhenNoProviderConfigured() {
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

        CheckDraftResult result = service.checkDraft(sampleRequest());

        assertThat(result.fallback()).isTrue();
        assertThat(result.issues()).isEmpty();
        assertThat(result.aiError()).isNull();
    }

    @Test
    void checkDraftReturnsParsedIssues() {
        when(moonshotModel.call(any(Prompt.class))).thenReturn(llmResponse(validCheckJson()));

        CheckDraftResult result = aiService.checkDraft(sampleRequest());

        assertThat(result.fallback()).isFalse();
        assertThat(result.issues()).containsExactly("⚠️ 未提及合作Brief链接或内容说明。");
    }

    @Test
    void checkDraftFallsBackToSecondaryProvider() {
        when(moonshotModel.call(any(Prompt.class))).thenThrow(new RuntimeException("503"));
        when(deepseekModel.call(any(Prompt.class))).thenReturn(llmResponse(validCheckJson()));

        CheckDraftResult result = aiService.checkDraft(sampleRequest());

        assertThat(result.fallback()).isFalse();
        assertThat(result.issues()).hasSize(1);
        verify(deepseekModel).call(any(Prompt.class));
    }

    @Test
    void checkDraftReturnsEmptyIssuesWhenAllProvidersFail() {
        when(moonshotModel.call(any(Prompt.class))).thenThrow(new RuntimeException("timeout"));
        when(deepseekModel.call(any(Prompt.class))).thenThrow(new RuntimeException("401"));

        CheckDraftResult result = aiService.checkDraft(sampleRequest());

        assertThat(result.fallback()).isTrue();
        assertThat(result.issues()).isEmpty();
        assertThat(result.aiError()).isEqualTo("AI 草稿检查失败");
    }

    private static CheckDraftRequest sampleRequest() {
        return new CheckDraftRequest(
                "Hi Alice, looking forward to working together.", KolStage.CONFIRMED, "youtube", "Alice");
    }

    private static String validCheckJson() {
        return """
                {
                  "issues": ["⚠️ 未提及合作Brief链接或内容说明。"]
                }
                """;
    }

    private static ChatResponse llmResponse(String content) {
        return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(content))))
                .build();
    }
}

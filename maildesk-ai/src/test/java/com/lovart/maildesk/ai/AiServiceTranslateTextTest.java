package com.lovart.maildesk.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.ai.check.CheckDraftParser;
import com.lovart.maildesk.ai.classify.EmailClassificationParser;
import com.lovart.maildesk.ai.fallback.AiInvocationPipeline;
import com.lovart.maildesk.ai.config.AiProviderProperties;
import com.lovart.maildesk.ai.draft.ReplyDraftParser;
import com.lovart.maildesk.ai.prompt.AiPromptCatalog;
import com.lovart.maildesk.ai.translate.TranslateTargetLang;
import com.lovart.maildesk.ai.translate.TranslateTextRequest;
import com.lovart.maildesk.ai.translate.TranslateTextResult;
import com.lovart.maildesk.ai.translate.TranslateMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceTranslateTextTest {

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

        AiProviderProperties.CapabilityBinding translate = new AiProviderProperties.CapabilityBinding();
        translate.setProvider("moonshot");
        translate.setModel("moonshot-v1-8k");
        translate.setFallbackProvider("deepseek");
        translate.setFallbackModel("deepseek-v4-flash");
        properties.setCapabilities(Map.of("translate", translate));

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
    void translateTextReturnsNotConfiguredMessageWhenNoProvider() {
        ObjectMapper objectMapper = new ObjectMapper();
        AiService service = new AiService(
                new AiInvocationPipeline(new AiModelRouter(new AiProviderProperties(), Map.of())),
                new AiPromptCatalog(),
                new EmailClassificationParser(objectMapper),
                new ReplyDraftParser(objectMapper),
                new CheckDraftParser(objectMapper),
                objectMapper);

        TranslateTextResult result = service.translateText(sampleRequest());

        assertThat(result.fallback()).isTrue();
        assertThat(result.translated()).contains("AI 翻译未配置");
        assertThat(result.aiError()).isNull();
    }

    @Test
    void translateTextReturnsLlmOutputForChineseTarget() {
        when(moonshotModel.call(any(Prompt.class)))
                .thenReturn(llmResponse("这是邮件的中文翻译。"));

        TranslateTextResult result = aiService.translateText(sampleRequest());

        assertThat(result.fallback()).isFalse();
        assertThat(result.translated()).isEqualTo("这是邮件的中文翻译。");
        assertThat(result.targetLang()).isEqualTo(TranslateTargetLang.ZH);
    }

    @Test
    void translateTextUsesEnglishPromptForEnTarget() {
        when(moonshotModel.call(any(Prompt.class))).thenReturn(llmResponse("Hello team,"));

        TranslateTextRequest request = new TranslateTextRequest(
                "你好，团队", TranslateTargetLang.EN, TranslateMode.SEND_DRAFT);
        TranslateTextResult result = aiService.translateText(request);

        assertThat(result.translated()).isEqualTo("Hello team,");
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(moonshotModel).call(captor.capture());
        assertThat(captor.getValue().getInstructions().get(0).getText())
                .contains("native-level English");
    }

    @Test
    void translateTextFallsBackToSecondaryProvider() {
        when(moonshotModel.call(any(Prompt.class))).thenThrow(new RuntimeException("429"));
        when(deepseekModel.call(any(Prompt.class))).thenReturn(llmResponse("中文译文"));

        TranslateTextResult result = aiService.translateText(sampleRequest());

        assertThat(result.fallback()).isFalse();
        assertThat(result.translated()).isEqualTo("中文译文");
        verify(deepseekModel).call(any(Prompt.class));
    }

    @Test
    void translateTextReturnsFailureMessageWhenAllProvidersFail() {
        when(moonshotModel.call(any(Prompt.class))).thenThrow(new RuntimeException("timeout"));
        when(deepseekModel.call(any(Prompt.class))).thenThrow(new RuntimeException("401"));

        TranslateTextResult result = aiService.translateText(sampleRequest());

        assertThat(result.fallback()).isTrue();
        assertThat(result.translated()).contains("AI 翻译失败");
        assertThat(result.aiError()).isEqualTo("AI 翻译失败");
    }

    @Test
    void translateTextUpgradesTo32kForLongInput() {
        when(moonshotModel.call(any(Prompt.class))).thenReturn(llmResponse("长文翻译"));

        String longText = "a".repeat(6000);
        aiService.translateText(new TranslateTextRequest(longText, TranslateTargetLang.ZH));

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(moonshotModel).call(captor.capture());
        OpenAiChatOptions options = (OpenAiChatOptions) captor.getValue().getOptions();
        assertThat(options.getModel()).isEqualTo("moonshot-v1-32k");
    }

    private static TranslateTextRequest sampleRequest() {
        return new TranslateTextRequest("Thanks for your reply.", TranslateTargetLang.ZH, TranslateMode.EMAIL_BODY);
    }

    private static ChatResponse llmResponse(String content) {
        return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(content))))
                .build();
    }
}

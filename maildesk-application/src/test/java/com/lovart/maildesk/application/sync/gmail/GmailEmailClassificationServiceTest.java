package com.lovart.maildesk.application.sync.gmail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.lovart.maildesk.ai.AiModelRouter;
import com.lovart.maildesk.ai.AiService;
import com.lovart.maildesk.ai.check.CheckDraftParser;
import com.lovart.maildesk.ai.classify.ClassifyEmailRequest;
import com.lovart.maildesk.ai.classify.EmailClassificationParser;
import com.lovart.maildesk.ai.classify.EmailClassificationResult;
import com.lovart.maildesk.ai.config.AiProviderProperties;
import com.lovart.maildesk.ai.draft.ReplyDraftParser;
import com.lovart.maildesk.ai.fallback.AiInvocationPipeline;
import com.lovart.maildesk.ai.fallback.AiUserMessages;
import com.lovart.maildesk.ai.prompt.AiPromptCatalog;
import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.domain.gmail.GmailFullMessage;
import com.lovart.maildesk.integration.gmail.GmailProperties;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmailEmailClassificationServiceTest {

    @Mock
    private ChatModel moonshotModel;

    private GmailEmailClassificationService service;

    private static GmailProperties gmailProperties(boolean aiClassificationEnabled) {
        return new GmailProperties(
                "client-id",
                "client-secret",
                java.time.Duration.ofSeconds(5),
                java.time.Duration.ofSeconds(5),
                1,
                aiClassificationEnabled);
    }

    private AiService buildAiService() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setDefaultProvider("moonshot");
        AiProviderProperties.Provider moonshot = new AiProviderProperties.Provider();
        moonshot.setBaseUrl("https://api.moonshot.cn/v1");
        moonshot.setApiKey("moonshot-key");
        properties.setProviders(Map.of("moonshot", moonshot));
        AiProviderProperties.CapabilityBinding classify = new AiProviderProperties.CapabilityBinding();
        classify.setProvider("moonshot");
        classify.setModel("moonshot-v1-8k");
        properties.setCapabilities(Map.of("classify", classify));

        ObjectMapper objectMapper = new ObjectMapper();
        AiModelRouter router = new AiModelRouter(properties, Map.of("moonshot", moonshotModel));
        return new AiService(
                new AiInvocationPipeline(router),
                new AiPromptCatalog(),
                new EmailClassificationParser(objectMapper),
                new ReplyDraftParser(objectMapper),
                new CheckDraftParser(objectMapper),
                objectMapper);
    }

    @BeforeEach
    void setUp() {
        service = new GmailEmailClassificationService(buildAiService(), gmailProperties(true));
    }

    @Test
    void usesDirectionOnlyWhenSyncAiDisabled() {
        GmailEmailClassificationService disabled =
                new GmailEmailClassificationService(buildAiService(), gmailProperties(false));

        GmailAiFallback.GmailAiFields inbound = disabled.classify(sampleMessage(), EmailDirection.INBOUND);
        GmailAiFallback.GmailAiFields outbound =
                disabled.classify(sampleMessage(), EmailDirection.OUTBOUND);

        assertThat(inbound.stageSignal()).isEqualTo(KolStage.REPLIED);
        assertThat(inbound.summary()).isEqualTo("客户来信，待回复");
        assertThat(inbound.aiError()).isNull();
        assertThat(outbound.stageSignal()).isEqualTo(KolStage.OUTREACH);
        assertThat(outbound.summary()).isEqualTo("我方已发送，等待对方回复");
    }

    @Test
    void forceAiBypassesSyncDisableFlag() {
        GmailEmailClassificationService disabled =
                new GmailEmailClassificationService(buildAiService(), gmailProperties(false));
        when(moonshotModel.call(any(Prompt.class)))
                .thenReturn(llmResponse(
                        """
                        {
                          "stage_signal": "negotiating",
                          "priority": "high",
                          "summary": "报价沟通",
                          "extracted": {},
                          "suggested_action": "确认报价"
                        }
                        """));

        GmailAiFallback.GmailAiFields fields =
                disabled.classify(sampleMessage(), EmailDirection.INBOUND, true);

        assertThat(fields.stageSignal()).isEqualTo(KolStage.NEGOTIATING);
        verify(moonshotModel).call(any(Prompt.class));
    }

    @Test
    void mapsSuccessfulClassification() {
        when(moonshotModel.call(any(Prompt.class)))
                .thenReturn(llmResponse(
                        """
                        {
                          "stage_signal": "negotiating",
                          "priority": "high",
                          "summary": "报价沟通",
                          "extracted": {},
                          "suggested_action": "确认报价"
                        }
                        """));

        GmailAiFallback.GmailAiFields fields = service.classify(sampleMessage(), EmailDirection.INBOUND);

        assertThat(fields.stageSignal()).isEqualTo(KolStage.NEGOTIATING);
        assertThat(fields.priority()).isEqualTo("high");
        assertThat(fields.bodyZh()).isNull();
        assertThat(fields.aiError()).isNull();
    }

    @Test
    void mapsAiFailureWithoutThrowing() {
        when(moonshotModel.call(any(Prompt.class))).thenThrow(new RuntimeException("401 unauthorized"));

        GmailAiFallback.GmailAiFields fields = service.classify(sampleMessage(), EmailDirection.INBOUND);

        assertThat(fields.summary()).isEqualTo(AiUserMessages.CLASSIFY_FAILURE_SUMMARY);
        assertThat(fields.aiError()).isEqualTo(AiUserMessages.CLASSIFY_AI_ERROR);
        assertThat(fields.bodyZh()).isNull();
    }

    @Test
    void passesMessageBodyToClassifier() {
        when(moonshotModel.call(any(Prompt.class)))
                .thenReturn(llmResponse(
                        """
                        {
                          "stage_signal": "replied",
                          "priority": "medium",
                          "summary": "已回复",
                          "extracted": {},
                          "suggested_action": "跟进"
                        }
                        """));

        service.classify(sampleMessage(), EmailDirection.INBOUND);

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(moonshotModel).call(captor.capture());
        assertThat(captor.getValue().getInstructions().get(1).getText()).contains("\"body\":\"body\"");
    }

    private static GmailFullMessage sampleMessage() {
        return new GmailFullMessage(
                "msg-1",
                "thread-1",
                "1001",
                "creator@example.com",
                List.of("me@company.com"),
                List.of(),
                "Rate inquiry",
                "body",
                null,
                List.of(),
                false,
                OffsetDateTime.now(ZoneOffset.UTC),
                List.of("UNREAD"));
    }

    private static ChatResponse llmResponse(String content) {
        return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(content))))
                .build();
    }
}

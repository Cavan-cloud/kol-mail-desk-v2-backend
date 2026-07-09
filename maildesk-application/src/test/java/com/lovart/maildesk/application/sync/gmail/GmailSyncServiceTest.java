package com.lovart.maildesk.application.sync.gmail;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.ai.AiModelRouter;
import com.lovart.maildesk.ai.AiService;
import com.lovart.maildesk.ai.check.CheckDraftParser;
import com.lovart.maildesk.ai.classify.EmailClassificationParser;
import com.lovart.maildesk.ai.config.AiProviderProperties;
import com.lovart.maildesk.ai.draft.ReplyDraftParser;
import com.lovart.maildesk.ai.fallback.AiInvocationPipeline;
import com.lovart.maildesk.ai.prompt.AiPromptCatalog;
import com.lovart.maildesk.application.observability.MaildeskMetrics;
import com.lovart.maildesk.domain.credential.GoogleAccessToken;
import com.lovart.maildesk.domain.credential.GoogleCredentialPort;
import com.lovart.maildesk.domain.email.entity.EmailDO;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.gmail.GmailClient;
import com.lovart.maildesk.domain.gmail.GmailFullMessage;
import com.lovart.maildesk.domain.gmail.GmailHistoryPage;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import com.lovart.maildesk.integration.gmail.GmailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmailSyncServiceTest {

    @Mock
    private GmailClient gmailClient;

    @Mock
    private GoogleCredentialPort credentials;

    @Mock
    private ProfileMapper profiles;

    @Mock
    private KolMapper kols;

    @Mock
    private EmailMapper emails;

    @Mock
    private ChatModel moonshotModel;

    @Mock
    private ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterRegistryProvider;

    private GmailEmailClassificationService classificationService;
    private GmailPersistService persistService;
    private GmailSyncService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        when(meterRegistryProvider.getIfAvailable()).thenReturn(null);
        MaildeskMetrics metrics = new MaildeskMetrics(meterRegistryProvider);
        classificationService = new GmailEmailClassificationService(buildAiService(), gmailProperties(true));
        persistService = new GmailPersistService(kols, emails, new ObjectMapper());
        service = new GmailSyncService(
                gmailClient, credentials, profiles, emails, persistService, classificationService, metrics);
        userId = UUID.randomUUID();
    }

    @Test
    void sync_returnsNotConfiguredWhenCredentialMissing() {
        ProfileDO profile = profile();
        when(profiles.selectById(userId)).thenReturn(profile);
        when(credentials.resolveAccessToken(userId)).thenReturn(Optional.empty());

        GmailSyncResult result = service.sync(userId, GmailSyncOptions.incremental());

        assertThat(result.status()).isEqualTo("not_configured");
    }

    @Test
    void sync_incrementalUsesHistoryListAndPersists() {
        when(moonshotModel.call(any(Prompt.class)))
                .thenReturn(llmResponse(
                        """
                        {
                          "stage_signal": "replied",
                          "priority": "medium",
                          "summary": "summary",
                          "extracted": {},
                          "suggested_action": "action"
                        }
                        """));

        ProfileDO profile = profile();
        profile.setId(userId);
        profile.setLastSyncedHistoryId("9000");
        when(profiles.selectById(userId)).thenReturn(profile);
        when(credentials.resolveAccessToken(userId)).thenReturn(Optional.of(new GoogleAccessToken("token")));
        when(gmailClient.listIncrementalMessageIds("token", "9000", 50))
                .thenReturn(new GmailHistoryPage(List.of("msg-1"), "history"));
        when(emails.selectList(any())).thenReturn(List.of());
        when(gmailClient.getMessage("token", "msg-1")).thenReturn(sampleMessage("msg-1", "9001"));
        KolDO kol = new KolDO();
        kol.setId(UUID.randomUUID());
        kol.setEmail("creator@example.com");
        kol.setSource("feishu");
        kol.setFeishuRecordId("rec-1");
        kol.setOwnerUserId(userId);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(kol));
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        GmailSyncResult result = service.sync(userId, GmailSyncOptions.incremental());

        assertThat(result.status()).isEqualTo("synced");
        assertThat(result.processed()).isEqualTo(1);
        assertThat(result.insertedEmails()).isEqualTo(1);
        verify(profiles).updateById(any(ProfileDO.class));
    }

    @Test
    void sync_completesWhenAiClassificationFails() {
        when(moonshotModel.call(any(Prompt.class))).thenThrow(new RuntimeException("401 unauthorized"));

        ProfileDO profile = profile();
        profile.setId(userId);
        profile.setLastSyncedHistoryId("9000");
        when(profiles.selectById(userId)).thenReturn(profile);
        when(credentials.resolveAccessToken(userId)).thenReturn(Optional.of(new GoogleAccessToken("token")));
        when(gmailClient.listIncrementalMessageIds("token", "9000", 50))
                .thenReturn(new GmailHistoryPage(List.of("msg-1"), "history"));
        when(emails.selectList(any())).thenReturn(List.of());
        when(gmailClient.getMessage("token", "msg-1")).thenReturn(sampleMessage("msg-1", "9001"));
        KolDO kol = new KolDO();
        kol.setId(UUID.randomUUID());
        kol.setEmail("creator@example.com");
        kol.setSource("feishu");
        kol.setFeishuRecordId("rec-1");
        kol.setOwnerUserId(userId);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(kol));
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        GmailSyncResult result = service.sync(userId, GmailSyncOptions.incremental());

        assertThat(result.status()).isEqualTo("synced");
        assertThat(result.insertedEmails()).isEqualTo(1);
    }

    @Test
    void sync_skipsAiClassificationForKnownMessageIds() {
        ProfileDO profile = profile();
        profile.setId(userId);
        profile.setLastSyncedHistoryId("9000");
        when(profiles.selectById(userId)).thenReturn(profile);
        when(credentials.resolveAccessToken(userId)).thenReturn(Optional.of(new GoogleAccessToken("token")));
        when(gmailClient.listIncrementalMessageIds("token", "9000", 50))
                .thenReturn(new GmailHistoryPage(List.of("msg-known"), "history"));
        when(gmailClient.getMessage("token", "msg-known")).thenReturn(sampleMessage("msg-known", "9001"));

        EmailDO existing = new EmailDO();
        existing.setGmailMessageId("msg-known");
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(existing));

        KolDO kol = new KolDO();
        kol.setId(UUID.randomUUID());
        kol.setEmail("creator@example.com");
        kol.setSource("feishu");
        kol.setFeishuRecordId("rec-1");
        kol.setOwnerUserId(userId);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(kol));

        GmailSyncResult result = service.sync(userId, GmailSyncOptions.incremental());

        assertThat(result.status()).isEqualTo("synced");
        verify(moonshotModel, never()).call(any(Prompt.class));
    }

    @Test
    void sync_skipsLlmWhenAiClassificationDisabled() {
        MaildeskMetrics metrics = new MaildeskMetrics(meterRegistryProvider);
        classificationService = new GmailEmailClassificationService(buildAiService(), gmailProperties(false));
        service = new GmailSyncService(
                gmailClient, credentials, profiles, emails, persistService, classificationService, metrics);

        ProfileDO profile = profile();
        profile.setId(userId);
        profile.setLastSyncedHistoryId("9000");
        when(profiles.selectById(userId)).thenReturn(profile);
        when(credentials.resolveAccessToken(userId)).thenReturn(Optional.of(new GoogleAccessToken("token")));
        when(gmailClient.listIncrementalMessageIds("token", "9000", 50))
                .thenReturn(new GmailHistoryPage(List.of("msg-new"), "history"));
        when(gmailClient.getMessage("token", "msg-new")).thenReturn(sampleMessage("msg-new", "9002"));
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        KolDO kol = new KolDO();
        kol.setId(UUID.randomUUID());
        kol.setEmail("creator@example.com");
        kol.setSource("feishu");
        kol.setFeishuRecordId("rec-1");
        kol.setOwnerUserId(userId);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(kol));

        GmailSyncResult result = service.sync(userId, GmailSyncOptions.incremental());

        assertThat(result.status()).isEqualTo("synced");
        assertThat(result.insertedEmails()).isEqualTo(1);
        verify(moonshotModel, never()).call(any(Prompt.class));
    }

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

    private static ProfileDO profile() {
        ProfileDO profile = new ProfileDO();
        profile.setId(UUID.randomUUID());
        profile.setEmail("me@company.com");
        profile.setFeishuOperatorName("运营A");
        profile.setLastSyncedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return profile;
    }

    private static GmailFullMessage sampleMessage(String id, String historyId) {
        return new GmailFullMessage(
                id,
                "thread-1",
                historyId,
                "creator@example.com",
                List.of("me@company.com"),
                List.of(),
                "subject",
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

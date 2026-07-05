package com.lovart.maildesk.application.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.ai.AiModelRouter;
import com.lovart.maildesk.ai.AiService;
import com.lovart.maildesk.ai.check.CheckDraftParser;
import com.lovart.maildesk.ai.classify.EmailClassificationParser;
import com.lovart.maildesk.ai.config.AiProviderProperties;
import com.lovart.maildesk.ai.draft.ReplyDraftParser;
import com.lovart.maildesk.ai.fallback.AiInvocationPipeline;
import com.lovart.maildesk.ai.fallback.AiUserMessages;
import com.lovart.maildesk.ai.prompt.AiPromptCatalog;
import com.lovart.maildesk.application.audit.AuditLogService;
import com.lovart.maildesk.application.dto.EmailDto;
import com.lovart.maildesk.application.dto.EmailUpdateRequest;
import com.lovart.maildesk.application.sync.gmail.GmailEmailClassificationService;
import com.lovart.maildesk.common.enums.ActionType;
import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.domain.audit.entity.ActionDO;
import com.lovart.maildesk.domain.audit.mapper.ActionMapper;
import com.lovart.maildesk.domain.email.entity.EmailDO;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailApplicationServiceTest {

    @Mock
    private EmailMapper emails;

    @Mock
    private KolMapper kols;

    @Mock
    private ProfileMapper profiles;

    @Mock
    private ActionMapper actions;

    @Mock
    private ChatModel moonshotModel;

    private AuditLogService auditLog;
    private EmailApplicationService service;
    private UUID userId;
    private UUID emailId;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLogService(actions);
        GmailEmailClassificationService classificationService =
                new GmailEmailClassificationService(buildAiService());
        service = new EmailApplicationService(emails, kols, profiles, classificationService, auditLog);
        userId = UUID.randomUUID();
        emailId = UUID.randomUUID();
    }

    @Test
    void updateEmail_marksReadAndAudits() {
        when(emails.selectOne(any())).thenReturn(sampleEmail());

        EmailDto result = service.updateEmail(userId, emailId, new EmailUpdateRequest(true));

        assertThat(result.isRead()).isTrue();
        ArgumentCaptor<EmailDO> captor = ArgumentCaptor.forClass(EmailDO.class);
        verify(emails).updateById(captor.capture());
        assertThat(captor.getValue().getIsRead()).isTrue();
        assertThat(captor.getValue().getReadAt()).isNotNull();

        ArgumentCaptor<ActionDO> auditCaptor = ArgumentCaptor.forClass(ActionDO.class);
        verify(actions).insert(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getActionType()).isEqualTo(ActionType.EMAIL_READ);
        assertThat(auditCaptor.getValue().getMetadata().get("is_read").asBoolean()).isTrue();
    }

    @Test
    void updateEmail_marksUnread() {
        EmailDO email = sampleEmail();
        email.setIsRead(true);
        when(emails.selectOne(any())).thenReturn(email);

        EmailDto result = service.updateEmail(userId, emailId, new EmailUpdateRequest(false));

        assertThat(result.isRead()).isFalse();
        ArgumentCaptor<EmailDO> captor = ArgumentCaptor.forClass(EmailDO.class);
        verify(emails).updateById(captor.capture());
        assertThat(captor.getValue().getIsRead()).isFalse();
        assertThat(captor.getValue().getReadAt()).isNull();
    }

    @Test
    void updateEmail_rejectsMissingEmail() {
        when(emails.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.updateEmail(userId, emailId, new EmailUpdateRequest(true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮件不存在");
    }

    @Test
    void deleteEmail_softDeletesAndRemovesEmptyKol() {
        UUID kolId = UUID.randomUUID();
        EmailDO email = sampleEmail();
        email.setKolId(kolId);
        when(emails.selectOne(any())).thenReturn(email);
        when(emails.selectCount(any())).thenReturn(0L);

        service.deleteEmail(userId, emailId);

        verify(emails).deleteById(emailId);
        verify(kols).deleteById(kolId);
    }

    @Test
    void deleteEmail_keepsKolWhenOtherEmailsRemain() {
        UUID kolId = UUID.randomUUID();
        EmailDO email = sampleEmail();
        email.setKolId(kolId);
        when(emails.selectOne(any())).thenReturn(email);
        when(emails.selectCount(any())).thenReturn(2L);

        service.deleteEmail(userId, emailId);

        verify(emails).deleteById(emailId);
        verify(kols, org.mockito.Mockito.never()).deleteById(kolId);
    }

    @Test
    void deleteEmail_allowsLeaderToDeleteOthersEmail() {
        UUID ownerId = UUID.randomUUID();
        EmailDO email = sampleEmail();
        email.setUserId(ownerId);
        ProfileDO leader = new ProfileDO();
        leader.setId(userId);
        leader.setRole("leader");
        when(emails.selectOne(any())).thenReturn(email);
        when(profiles.selectById(userId)).thenReturn(leader);
        when(emails.selectCount(any())).thenReturn(1L);

        service.deleteEmail(userId, emailId);

        verify(emails).deleteById(emailId);
    }

    @Test
    void deleteEmail_rejectsNonOwnerNonLeader() {
        EmailDO email = sampleEmail();
        email.setUserId(UUID.randomUUID());
        ProfileDO member = new ProfileDO();
        member.setId(userId);
        member.setRole("member");
        when(emails.selectOne(any())).thenReturn(email);
        when(profiles.selectById(userId)).thenReturn(member);

        assertThatThrownBy(() -> service.deleteEmail(userId, emailId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void reclassify_updatesEmailWithSuccessfulClassification() {
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
        when(emails.selectOne(any())).thenReturn(sampleEmail());

        EmailDto result = service.reclassify(userId, emailId);

        assertThat(result.aiSummary()).isEqualTo("报价沟通");
        assertThat(result.aiPriority()).isEqualTo("high");
        ArgumentCaptor<EmailDO> captor = ArgumentCaptor.forClass(EmailDO.class);
        verify(emails).updateById(captor.capture());
        assertThat(captor.getValue().getAiStageSignal()).isEqualTo(KolStage.NEGOTIATING);
        assertThat(captor.getValue().getAiError()).isNull();
    }

    @Test
    void reclassify_persistsFallbackWhenAiFails() {
        when(moonshotModel.call(any(Prompt.class))).thenThrow(new RuntimeException("401 unauthorized"));
        when(emails.selectOne(any())).thenReturn(sampleEmail());

        EmailDto result = service.reclassify(userId, emailId);

        assertThat(result.aiSummary()).isEqualTo(AiUserMessages.CLASSIFY_FAILURE_SUMMARY);
        assertThat(result.aiSuggestedAction()).isEqualTo(AiUserMessages.CLASSIFY_FAILURE_ACTION);
        ArgumentCaptor<EmailDO> captor = ArgumentCaptor.forClass(EmailDO.class);
        verify(emails).updateById(captor.capture());
        assertThat(captor.getValue().getAiError()).isEqualTo(AiUserMessages.CLASSIFY_AI_ERROR);
    }

    @Test
    void reclassify_rejectsMissingEmail() {
        when(emails.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.reclassify(userId, emailId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮件不存在");
    }

    @Test
    void reclassify_rejectsEmptyBody() {
        EmailDO email = sampleEmail();
        email.setBodyText("  ");
        email.setBodyHtml(null);
        when(emails.selectOne(any())).thenReturn(email);

        assertThatThrownBy(() -> service.reclassify(userId, emailId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("正文为空");
    }

    private EmailDO sampleEmail() {
        EmailDO email = new EmailDO();
        email.setId(emailId);
        email.setUserId(userId);
        email.setKolId(UUID.randomUUID());
        email.setGmailMessageId("msg-1");
        email.setGmailThreadId("thread-1");
        email.setDirection(EmailDirection.INBOUND);
        email.setFromEmail("creator@example.com");
        email.setToEmails(List.of("me@company.com"));
        email.setSubject("Rate inquiry");
        email.setBodyText("Please share your rate card.");
        email.setSentAt(OffsetDateTime.now(ZoneOffset.UTC));
        return email;
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

    private static ChatResponse llmResponse(String content) {
        return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(content))))
                .build();
    }
}

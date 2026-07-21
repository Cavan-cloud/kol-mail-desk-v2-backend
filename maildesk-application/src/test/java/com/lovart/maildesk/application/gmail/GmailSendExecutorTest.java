package com.lovart.maildesk.application.gmail;

import com.lovart.maildesk.application.audit.AuditLogService;
import com.lovart.maildesk.application.dto.SendEmailRequest;
import com.lovart.maildesk.application.dto.SendEmailResultDto;
import com.lovart.maildesk.common.enums.ActionType;
import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.common.exception.GmailIntegrationException;
import com.lovart.maildesk.domain.audit.entity.ActionDO;
import com.lovart.maildesk.domain.audit.mapper.ActionMapper;
import com.lovart.maildesk.domain.credential.GoogleAccessToken;
import com.lovart.maildesk.domain.credential.GoogleCredentialPort;
import com.lovart.maildesk.domain.email.entity.EmailDO;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.gmail.GmailClient;
import com.lovart.maildesk.domain.gmail.GmailOutboundMessage;
import com.lovart.maildesk.domain.gmail.GmailSentMessage;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import com.lovart.maildesk.domain.template.entity.EmailTemplateDO;
import com.lovart.maildesk.domain.template.mapper.EmailTemplateMapper;
import com.lovart.maildesk.integration.gmail.GmailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmailSendExecutorTest {

    @Mock
    private KolMapper kols;

    @Mock
    private ProfileMapper profiles;

    @Mock
    private GoogleCredentialPort credentials;

    @Mock
    private GmailClient gmailClient;

    @Mock
    private EmailMapper emails;

    @Mock
    private EmailTemplateMapper templates;

    @Mock
    private ActionMapper actions;

    private GmailSendExecutor executor;
    private UUID userId;
    private UUID kolId;

    @BeforeEach
    void setUp() {
        AuditLogService auditLog = new AuditLogService(actions);
        GmailSendPersistenceService persistence =
                new GmailSendPersistenceService(emails, kols, templates, auditLog);
        GmailProperties properties =
                new GmailProperties("client-id", "client-secret", java.time.Duration.ofSeconds(5), java.time.Duration.ofSeconds(5), 1, true);
        executor = new GmailSendExecutor(
                kols, profiles, credentials, gmailClient, properties, persistence, auditLog);
        userId = UUID.randomUUID();
        kolId = UUID.randomUUID();
    }

    @Test
    void execute_returnsNotConfiguredWhenCredentialMissing() {
        KolDO kol = new KolDO();
        kol.setId(kolId);
        when(kols.selectById(kolId)).thenReturn(kol);
        when(credentials.hasCredential(userId)).thenReturn(false);

        SendEmailResultDto result = executor.execute(userId, sampleRequest(null));

        assertThat(result.status()).isEqualTo("not_configured");
        verify(emails, never()).insert(any(EmailDO.class));
        ArgumentCaptor<ActionDO> auditCaptor = ArgumentCaptor.forClass(ActionDO.class);
        verify(actions).insert(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getActionType()).isEqualTo(ActionType.EMAIL_SENT);
    }

    @Test
    void execute_persistsOutboundEmailAndUpdatesKol() {
        UUID templateId = UUID.randomUUID();
        KolDO kol = new KolDO();
        kol.setId(kolId);
        when(kols.selectById(kolId)).thenReturn(kol);
        when(credentials.hasCredential(userId)).thenReturn(true);
        when(credentials.hasGmailSendScope(userId)).thenReturn(true);
        when(credentials.resolveAccessToken(userId)).thenReturn(Optional.of(new GoogleAccessToken("access-token")));
        when(gmailClient.sendMessage(eq("access-token"), any(GmailOutboundMessage.class)))
                .thenReturn(new GmailSentMessage("msg-1", "thread-1"));
        ProfileDO profile = new ProfileDO();
        profile.setEmail("sender@company.com");
        when(profiles.selectById(userId)).thenReturn(profile);
        EmailTemplateDO template = new EmailTemplateDO();
        template.setId(templateId);
        template.setUsedCount(2);
        when(templates.selectById(templateId)).thenReturn(template);
        doAnswer(invocation -> {
            EmailDO email = invocation.getArgument(0);
            email.setId(UUID.randomUUID());
            return 1;
        }).when(emails).insert(any(EmailDO.class));

        SendEmailResultDto result = executor.execute(userId, sampleRequest(templateId));

        assertThat(result.status()).isEqualTo("sent");
        ArgumentCaptor<EmailDO> emailCaptor = ArgumentCaptor.forClass(EmailDO.class);
        verify(emails).insert(emailCaptor.capture());
        EmailDO saved = emailCaptor.getValue();
        assertThat(saved.getDirection()).isEqualTo(EmailDirection.OUTBOUND);
        assertThat(saved.getGmailMessageId()).isEqualTo("msg-1");
        assertThat(saved.getBodyHtml()).contains("<p>Hello</p>");
        assertThat(saved.getFromEmail()).isEqualTo("sender@company.com");

        ArgumentCaptor<KolDO> kolCaptor = ArgumentCaptor.forClass(KolDO.class);
        verify(kols).updateById(kolCaptor.capture());
        assertThat(kolCaptor.getValue().getLastOutboundAt()).isNotNull();
        assertThat(kolCaptor.getValue().getOwnerUserId()).isEqualTo(userId);

        ArgumentCaptor<EmailTemplateDO> templateCaptor = ArgumentCaptor.forClass(EmailTemplateDO.class);
        verify(templates).updateById(templateCaptor.capture());
        assertThat(templateCaptor.getValue().getUsedCount()).isEqualTo(3);
    }

    @Test
    void execute_doesNotStealExistingKolOwnerOnSend() {
        UUID existingOwner = UUID.randomUUID();
        KolDO kol = new KolDO();
        kol.setId(kolId);
        kol.setOwnerUserId(existingOwner);
        when(kols.selectById(kolId)).thenReturn(kol);
        when(credentials.hasCredential(userId)).thenReturn(true);
        when(credentials.hasGmailSendScope(userId)).thenReturn(true);
        when(credentials.resolveAccessToken(userId)).thenReturn(Optional.of(new GoogleAccessToken("access-token")));
        when(gmailClient.sendMessage(eq("access-token"), any(GmailOutboundMessage.class)))
                .thenReturn(new GmailSentMessage("msg-2", "thread-2"));
        ProfileDO profile = new ProfileDO();
        profile.setEmail("sender@company.com");
        when(profiles.selectById(userId)).thenReturn(profile);
        doAnswer(invocation -> {
            EmailDO email = invocation.getArgument(0);
            email.setId(UUID.randomUUID());
            return 1;
        }).when(emails).insert(any(EmailDO.class));

        SendEmailResultDto result = executor.execute(userId, sampleRequest(null));

        assertThat(result.status()).isEqualTo("sent");
        ArgumentCaptor<KolDO> kolCaptor = ArgumentCaptor.forClass(KolDO.class);
        verify(kols).updateById(kolCaptor.capture());
        assertThat(kolCaptor.getValue().getOwnerUserId()).isNull();
        assertThat(kolCaptor.getValue().getLastOutboundAt()).isNotNull();
    }

    @Test
    void execute_rejectsUnreviewedRequest() {
        SendEmailRequest request = new SendEmailRequest(
                kolId, "to@example.com", List.of(), "Subject", "Body", null, null, null, false);

        assertThatThrownBy(() -> executor.execute(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("人工审核");
    }

    @Test
    void execute_returnsFailedWhenGmailApiFails() {
        KolDO kol = new KolDO();
        kol.setId(kolId);
        when(kols.selectById(kolId)).thenReturn(kol);
        when(credentials.hasCredential(userId)).thenReturn(true);
        when(credentials.hasGmailSendScope(userId)).thenReturn(true);
        when(credentials.resolveAccessToken(userId)).thenReturn(Optional.of(new GoogleAccessToken("access-token")));
        when(gmailClient.sendMessage(eq("access-token"), any(GmailOutboundMessage.class)))
                .thenThrow(new GmailIntegrationException("Gmail API error"));

        SendEmailResultDto result = executor.execute(userId, sampleRequest(null));

        assertThat(result.status()).isEqualTo("failed");
        verify(emails, never()).insert(any(EmailDO.class));
    }

    private SendEmailRequest sampleRequest(UUID templateId) {
        return new SendEmailRequest(
                kolId,
                "to@example.com",
                List.of("cc@example.com"),
                "Subject",
                "Plain body",
                "<p>Hello</p>",
                "中文草稿",
                templateId,
                true);
    }
}

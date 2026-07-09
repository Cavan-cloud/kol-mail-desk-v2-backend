package com.lovart.maildesk.application.scheduled;

import com.lovart.maildesk.application.audit.AuditLogService;
import com.lovart.maildesk.application.dto.SendEmailResultDto;
import com.lovart.maildesk.application.gmail.GmailSendExecutor;
import com.lovart.maildesk.application.gmail.GmailSendPersistenceService;
import com.lovart.maildesk.common.enums.ScheduledEmailStatus;
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
import com.lovart.maildesk.domain.scheduled.entity.ScheduledEmailDO;
import com.lovart.maildesk.domain.scheduled.mapper.ScheduledEmailMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledEmailDispatchServiceTest {

    @Mock
    private ScheduledEmailMapper scheduledEmails;
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

    private ScheduledEmailDispatchService service;
    private UUID userId;
    private UUID kolId;
    private UUID scheduledId;

    @BeforeEach
    void setUp() {
        AuditLogService auditLog = new AuditLogService(actions);
        GmailSendPersistenceService persistence =
                new GmailSendPersistenceService(emails, kols, templates, auditLog);
        GmailProperties properties = new GmailProperties(
                "client-id", "client-secret", java.time.Duration.ofSeconds(5), java.time.Duration.ofSeconds(5), 1, true);
        GmailSendExecutor executor = new GmailSendExecutor(
                kols, profiles, credentials, gmailClient, properties, persistence, auditLog);
        service = new ScheduledEmailDispatchService(scheduledEmails, executor, null);
        userId = UUID.randomUUID();
        kolId = UUID.randomUUID();
        scheduledId = UUID.randomUUID();
    }

    @Test
    void dispatchClaimed_marksSentWhenGmailSucceeds() {
        ScheduledEmailDO row = sampleRow();
        stubSuccessfulSend();

        ScheduledEmailDispatchResult.ItemResult result = service.dispatchClaimed(row);

        assertThat(result.status()).isEqualTo("sent");
        ArgumentCaptor<ScheduledEmailDO> patch = ArgumentCaptor.forClass(ScheduledEmailDO.class);
        verify(scheduledEmails).updateById(patch.capture());
        assertThat(patch.getValue().getStatus()).isEqualTo(ScheduledEmailStatus.SENT.dbValue());
        assertThat(patch.getValue().getGmailMessageId()).isEqualTo("gmail-1");
    }

    @Test
    void dispatchClaimed_sendsHtmlBodyForMultipartAlternative() {
        ScheduledEmailDO row = sampleRow();
        stubSuccessfulSend();

        service.dispatchClaimed(row);

        ArgumentCaptor<GmailOutboundMessage> outbound = ArgumentCaptor.forClass(GmailOutboundMessage.class);
        verify(gmailClient).sendMessage(eq("access-token"), outbound.capture());
        assertThat(outbound.getValue().bodyHtml()).isEqualTo("<p>Plain</p>");
        assertThat(outbound.getValue().bodyText()).isEqualTo("Plain");
    }

    @Test
    void dispatchClaimed_marksFailedWhenNotConfigured() {
        ScheduledEmailDO row = sampleRow();
        KolDO kol = new KolDO();
        kol.setId(kolId);
        when(kols.selectById(kolId)).thenReturn(kol);
        when(credentials.hasCredential(userId)).thenReturn(false);

        ScheduledEmailDispatchResult.ItemResult result = service.dispatchClaimed(row);

        assertThat(result.status()).isEqualTo("failed");
        ArgumentCaptor<ScheduledEmailDO> patch = ArgumentCaptor.forClass(ScheduledEmailDO.class);
        verify(scheduledEmails).updateById(patch.capture());
        assertThat(patch.getValue().getStatus()).isEqualTo(ScheduledEmailStatus.FAILED.dbValue());
    }

    private void stubSuccessfulSend() {
        KolDO kol = new KolDO();
        kol.setId(kolId);
        when(kols.selectById(kolId)).thenReturn(kol);
        ProfileDO profile = new ProfileDO();
        profile.setId(userId);
        profile.setEmail("me@example.com");
        when(profiles.selectById(userId)).thenReturn(profile);
        when(credentials.hasCredential(userId)).thenReturn(true);
        when(credentials.hasGmailSendScope(userId)).thenReturn(true);
        when(credentials.resolveAccessToken(userId)).thenReturn(Optional.of(new GoogleAccessToken("access-token")));
        when(gmailClient.sendMessage(eq("access-token"), any(GmailOutboundMessage.class)))
                .thenReturn(new GmailSentMessage("gmail-1", "thread-1"));
        doAnswer(invocation -> {
            EmailDO email = invocation.getArgument(0);
            email.setId(UUID.randomUUID());
            return 1;
        }).when(emails).insert(any(EmailDO.class));
    }

    private ScheduledEmailDO sampleRow() {
        ScheduledEmailDO row = new ScheduledEmailDO();
        row.setId(scheduledId);
        row.setUserId(userId);
        row.setKolId(kolId);
        row.setToEmail("kol@example.com");
        row.setCcEmails(List.of("cc@example.com"));
        row.setSubject("Hi");
        row.setEnglishBody("Plain");
        row.setEnglishBodyHtml("<p>Plain</p>");
        row.setStatus(ScheduledEmailStatus.PROCESSING.dbValue());
        row.setAttemptCount(1);
        return row;
    }
}

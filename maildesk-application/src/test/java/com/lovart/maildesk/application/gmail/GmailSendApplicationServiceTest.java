package com.lovart.maildesk.application.gmail;

import com.lovart.maildesk.application.audit.AuditLogService;
import com.lovart.maildesk.application.dto.BatchSendRequest;
import com.lovart.maildesk.application.dto.BatchSendResultDto;
import com.lovart.maildesk.application.dto.SendEmailRequest;
import com.lovart.maildesk.application.dto.SendEmailResultDto;
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
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import com.lovart.maildesk.domain.template.mapper.EmailTemplateMapper;
import com.lovart.maildesk.integration.gmail.GmailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmailSendApplicationServiceTest {

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

    private GmailSendApplicationService service;
    private UUID userId;
    private UUID kolA;
    private UUID kolB;

    @BeforeEach
    void setUp() {
        AuditLogService auditLog = new AuditLogService(actions);
        GmailSendPersistenceService persistence =
                new GmailSendPersistenceService(emails, kols, templates, auditLog);
        GmailProperties properties =
                new GmailProperties("client-id", "client-secret", java.time.Duration.ofSeconds(5), java.time.Duration.ofSeconds(5), 1, true);
        GmailSendExecutor executor = new GmailSendExecutor(
                kols, profiles, credentials, gmailClient, properties, persistence, auditLog);
        service = new GmailSendApplicationService(executor);
        userId = UUID.randomUUID();
        kolA = UUID.randomUUID();
        kolB = UUID.randomUUID();
    }

    @Test
    void batchSend_runsSeriallyAndAggregatesCounts() {
        stubKol(kolA);
        stubKol(kolB);
        when(credentials.hasCredential(userId)).thenReturn(true);
        when(credentials.hasGmailSendScope(userId)).thenReturn(true);
        when(credentials.resolveAccessToken(userId)).thenReturn(Optional.of(new GoogleAccessToken("access-token")));
        doAnswer(invocation -> {
            EmailDO email = invocation.getArgument(0);
            email.setId(UUID.randomUUID());
            return 1;
        }).when(emails).insert(any(EmailDO.class));
        AtomicInteger sendCount = new AtomicInteger();
        when(gmailClient.sendMessage(eq("access-token"), any(GmailOutboundMessage.class)))
                .thenAnswer(invocation -> {
                    if (sendCount.incrementAndGet() == 1) {
                        return new GmailSentMessage("msg-1", "thread-1");
                    }
                    throw new com.lovart.maildesk.common.exception.GmailIntegrationException("boom");
                });

        BatchSendResultDto result = service.batchSend(
                userId,
                new BatchSendRequest(List.of(item(kolA), item(kolB))));

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.results()).hasSize(2);
        assertThat(sendCount.get()).isEqualTo(2);
    }

    @Test
    void send_delegatesToExecutor() {
        stubKol(kolA);
        when(credentials.hasCredential(userId)).thenReturn(false);

        SendEmailResultDto result = service.send(userId, item(kolA));

        assertThat(result.status()).isEqualTo("not_configured");
    }

    private void stubKol(UUID kolId) {
        KolDO kol = new KolDO();
        kol.setId(kolId);
        when(kols.selectById(kolId)).thenReturn(kol);
    }

    private SendEmailRequest item(UUID kolId) {
        return new SendEmailRequest(
                kolId,
                "to@example.com",
                List.of(),
                "Subject",
                "Body",
                null,
                null,
                null,
                true);
    }
}

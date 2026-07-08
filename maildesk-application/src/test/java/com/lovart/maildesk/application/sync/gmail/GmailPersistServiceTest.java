package com.lovart.maildesk.application.sync.gmail;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.ai.fallback.AiUserMessages;
import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.domain.email.entity.EmailDO;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.gmail.GmailFullMessage;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmailPersistServiceTest {

    @Mock
    private KolMapper kols;

    @Mock
    private EmailMapper emails;

    private GmailPersistService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new GmailPersistService(kols, emails, new ObjectMapper());
        userId = UUID.randomUUID();
    }

    @Test
    void skipsNonFeishuBackedEmails() {
        KolDO noise = feishuKol("noise@gmail.com");
        noise.setSource("gmail");
        noise.setFeishuRecordId(null);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(noise));
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        GmailSyncMessageDraft draft = inboundDraft("noise@gmail.com", "msg-1");
        GmailPersistService.PersistResult result = service.persist(userId, "@运营A", List.of(draft));

        assertThat(result.insertedEmails()).isZero();
        verify(emails, never()).insert(any(EmailDO.class));
    }

    @Test
    void persistsFeishuBackedInboundAndClearsReplyResolved() {
        KolDO kol = feishuKol("creator@example.com");
        kol.setOwnerUserId(userId);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(kol));
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        GmailSyncMessageDraft draft = inboundDraft("creator@example.com", "msg-2");
        GmailPersistService.PersistResult result = service.persist(userId, "@运营A", List.of(draft));

        assertThat(result.insertedEmails()).isEqualTo(1);
        ArgumentCaptor<EmailDO> emailCaptor = ArgumentCaptor.forClass(EmailDO.class);
        verify(emails).insert(emailCaptor.capture());
        assertThat(emailCaptor.getValue().getIsRead()).isFalse();

        verify(kols).update(isNull(), any());
    }

    @Test
    void persistsAiErrorWhenClassificationFailed() {
        KolDO kol = feishuKol("creator@example.com");
        kol.setOwnerUserId(userId);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(kol));
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        GmailSyncMessageDraft draft = inboundDraftWithAiError("creator@example.com", "msg-ai-fail");
        service.persist(userId, "@运营A", List.of(draft));

        ArgumentCaptor<EmailDO> emailCaptor = ArgumentCaptor.forClass(EmailDO.class);
        verify(emails).insert(emailCaptor.capture());
        assertThat(emailCaptor.getValue().getAiError()).isEqualTo(AiUserMessages.CLASSIFY_AI_ERROR);
        assertThat(emailCaptor.getValue().getAiSummary()).isEqualTo(AiUserMessages.CLASSIFY_FAILURE_SUMMARY);
    }

    @Test
    void outboundInsertMarksReadTrue() {
        KolDO kol = feishuKol("creator@example.com");
        kol.setOwnerUserId(userId);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(kol));
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        OffsetDateTime sentAt = OffsetDateTime.now(ZoneOffset.UTC);
        GmailFullMessage parsed = new GmailFullMessage(
                "msg-out",
                "thread-1",
                "1001",
                "me@company.com",
                List.of("creator@example.com"),
                List.of(),
                "subject",
                "body",
                null,
                List.of(),
                false,
                sentAt,
                List.of());
        GmailSyncMessageDraft draft = new GmailSyncMessageDraft(
                parsed,
                EmailDirection.OUTBOUND,
                "creator@example.com",
                KolStage.OUTREACH,
                "medium",
                "summary",
                null,
                "action",
                null,
                false);

        service.persist(userId, "@运营A", List.of(draft));

        ArgumentCaptor<EmailDO> emailCaptor = ArgumentCaptor.forClass(EmailDO.class);
        verify(emails).insert(emailCaptor.capture());
        assertThat(emailCaptor.getValue().getIsRead()).isTrue();
    }

    @Test
    void updateExistingSkipsAiAndReadStateWhenClassificationSkipped() {
        KolDO kol = feishuKol("creator@example.com");
        kol.setOwnerUserId(userId);
        UUID existingEmailId = UUID.randomUUID();
        EmailDO existing = new EmailDO();
        existing.setId(existingEmailId);
        existing.setGmailMessageId("msg-known");
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(kol));
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(existing));

        GmailSyncMessageDraft draft = skippedExistingDraft("creator@example.com", "msg-known");
        service.persist(userId, "@运营A", List.of(draft));

        verify(emails).updateById(any(EmailDO.class));
        verify(emails, never()).update(isNull(), any());
        verify(kols).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    void resyncExistingInboundDoesNotClearReplyResolved() {
        KolDO kol = feishuKol("creator@example.com");
        kol.setOwnerUserId(userId);
        kol.setReplyResolved(true);
        UUID existingEmailId = UUID.randomUUID();
        EmailDO existing = new EmailDO();
        existing.setId(existingEmailId);
        existing.setGmailMessageId("msg-known");
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(kol));
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(existing));

        GmailSyncMessageDraft draft = skippedExistingDraft("creator@example.com", "msg-known");
        service.persist(userId, "@运营A", List.of(draft));

        ArgumentCaptor<UpdateWrapper<KolDO>> kolUpdate = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(kols).update(isNull(), kolUpdate.capture());
        String sqlSet = kolUpdate.getValue().getSqlSet();
        assertThat(sqlSet == null || !sqlSet.contains("reply_resolved")).isTrue();
    }

    private static GmailSyncMessageDraft skippedExistingDraft(String counterparty, String messageId) {
        OffsetDateTime sentAt = OffsetDateTime.now(ZoneOffset.UTC);
        GmailFullMessage parsed = new GmailFullMessage(
                messageId,
                "thread-1",
                "1004",
                counterparty,
                List.of("me@company.com"),
                List.of(),
                "updated subject",
                "updated body",
                null,
                List.of(),
                false,
                sentAt,
                List.of("UNREAD"));
        return new GmailSyncMessageDraft(
                parsed,
                EmailDirection.INBOUND,
                counterparty,
                null,
                null,
                null,
                null,
                null,
                null,
                true);
    }

    private static KolDO feishuKol(String email) {
        KolDO kol = new KolDO();
        kol.setId(UUID.randomUUID());
        kol.setEmail(email);
        kol.setSource("feishu");
        kol.setFeishuRecordId("rec-1");
        kol.setFeishuOperatorName("运营A");
        return kol;
    }

    @Test
    void doesNotCreateGmailDuplicateWhenOperatorUnset() {
        KolDO feishuKol = feishuKol("creator@example.com");
        feishuKol.setFeishuOperatorName("潘慧妍");
        feishuKol.setOwnerUserId(null);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(feishuKol));
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        GmailSyncMessageDraft draft = inboundDraft("creator@example.com", "msg-unset-op");
        service.persist(userId, null, List.of(draft));

        verify(kols, never()).insert(any(KolDO.class));
        ArgumentCaptor<EmailDO> emailCaptor = ArgumentCaptor.forClass(EmailDO.class);
        verify(emails).insert(emailCaptor.capture());
        assertThat(emailCaptor.getValue().getKolId()).isEqualTo(feishuKol.getId());
        verify(kols).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    void autoClaimsFeishuRowWhenOperatorMatches() {
        KolDO feishuKol = feishuKol("creator@example.com");
        feishuKol.setFeishuOperatorName("潘慧妍");
        feishuKol.setOwnerUserId(null);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(feishuKol));
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.persist(userId, "潘慧妍", List.of(inboundDraft("creator@example.com", "msg-claim")));

        verify(kols, never()).insert(any(KolDO.class));
        ArgumentCaptor<UpdateWrapper<KolDO>> kolUpdate = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(kols).update(isNull(), kolUpdate.capture());
        assertThat(kolUpdate.getValue().getSqlSet()).contains("owner_user_id");
    }

    @Test
    void attachesToFeishuRowWithoutClaimWhenOperatorMismatches() {
        KolDO feishuKol = feishuKol("creator@example.com");
        feishuKol.setFeishuOperatorName("其他人");
        feishuKol.setOwnerUserId(null);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(feishuKol));
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.persist(userId, "潘慧妍", List.of(inboundDraft("creator@example.com", "msg-mismatch")));

        verify(kols, never()).insert(any(KolDO.class));
        ArgumentCaptor<UpdateWrapper<KolDO>> kolUpdate = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(kols).update(isNull(), kolUpdate.capture());
        String sqlSet = kolUpdate.getValue().getSqlSet();
        assertThat(sqlSet == null || !sqlSet.contains("owner_user_id")).isTrue();
    }

    private static GmailSyncMessageDraft inboundDraft(String counterparty, String messageId) {
        OffsetDateTime sentAt = OffsetDateTime.now(ZoneOffset.UTC);
        GmailFullMessage parsed = new GmailFullMessage(
                messageId,
                "thread-1",
                "1002",
                counterparty,
                List.of("me@company.com"),
                List.of(),
                "subject",
                "body",
                null,
                List.of(),
                false,
                sentAt,
                List.of("UNREAD"));
        return new GmailSyncMessageDraft(
                parsed,
                EmailDirection.INBOUND,
                counterparty,
                KolStage.REPLIED,
                "medium",
                "summary",
                "zh",
                "action",
                null,
                false);
    }

    private static GmailSyncMessageDraft inboundDraftWithAiError(String counterparty, String messageId) {
        OffsetDateTime sentAt = OffsetDateTime.now(ZoneOffset.UTC);
        GmailFullMessage parsed = new GmailFullMessage(
                messageId,
                "thread-1",
                "1003",
                counterparty,
                List.of("me@company.com"),
                List.of(),
                "subject",
                "body",
                null,
                List.of(),
                false,
                sentAt,
                List.of("UNREAD"));
        return new GmailSyncMessageDraft(
                parsed,
                EmailDirection.INBOUND,
                counterparty,
                KolStage.REPLIED,
                "medium",
                AiUserMessages.CLASSIFY_FAILURE_SUMMARY,
                null,
                AiUserMessages.CLASSIFY_FAILURE_ACTION,
                AiUserMessages.CLASSIFY_AI_ERROR,
                false);
    }
}

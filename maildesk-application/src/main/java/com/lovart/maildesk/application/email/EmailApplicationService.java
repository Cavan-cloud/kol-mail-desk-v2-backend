package com.lovart.maildesk.application.email;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.audit.AuditLogService;
import com.lovart.maildesk.application.dto.EmailDto;
import com.lovart.maildesk.application.dto.EmailUpdateRequest;
import com.lovart.maildesk.application.support.EntityMappers;
import com.lovart.maildesk.application.sync.gmail.GmailAiFallback;
import com.lovart.maildesk.application.sync.gmail.GmailEmailClassificationService;
import com.lovart.maildesk.common.enums.ActionType;
import com.lovart.maildesk.common.enums.UserRole;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.domain.email.entity.EmailDO;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.gmail.GmailFullMessage;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EmailApplicationService {

    private final EmailMapper emails;
    private final KolMapper kols;
    private final ProfileMapper profiles;
    private final GmailEmailClassificationService classificationService;
    private final AuditLogService auditLog;

    public EmailApplicationService(
            EmailMapper emails,
            KolMapper kols,
            ProfileMapper profiles,
            GmailEmailClassificationService classificationService,
            AuditLogService auditLog) {
        this.emails = emails;
        this.kols = kols;
        this.profiles = profiles;
        this.classificationService = classificationService;
        this.auditLog = auditLog;
    }

    @Transactional(rollbackFor = Exception.class)
    public EmailDto updateEmail(UUID userId, UUID emailId, EmailUpdateRequest request) {
        EmailDO existing = emails.selectOne(new LambdaQueryWrapper<EmailDO>()
                .eq(EmailDO::getId, emailId)
                .eq(EmailDO::getUserId, userId));
        if (existing == null) {
            throw new BusinessException("NOT_FOUND", "邮件不存在或已删除");
        }

        boolean isRead = Boolean.TRUE.equals(request.isRead());
        EmailDO patch = new EmailDO();
        patch.setId(emailId);
        patch.setIsRead(isRead);
        patch.setReadAt(isRead ? OffsetDateTime.now(ZoneOffset.UTC) : null);
        emails.updateById(patch);

        auditLog.append(ActionType.EMAIL_READ, "email", emailId, Map.of("is_read", isRead));

        existing.setIsRead(isRead);
        existing.setReadAt(patch.getReadAt());
        return EntityMappers.toEmailDto(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteEmail(UUID userId, UUID emailId) {
        EmailDO existing = emails.selectOne(new LambdaQueryWrapper<EmailDO>()
                .eq(EmailDO::getId, emailId));
        if (existing == null) {
            throw new BusinessException("NOT_FOUND", "邮件不存在或已删除");
        }
        assertCanDelete(userId, existing);

        emails.deleteById(emailId);

        UUID kolId = existing.getKolId();
        if (kolId != null) {
            Long remaining = emails.selectCount(new LambdaQueryWrapper<EmailDO>().eq(EmailDO::getKolId, kolId));
            if (remaining == 0) {
                kols.deleteById(kolId);
            }
        }
    }

    private void assertCanDelete(UUID userId, EmailDO email) {
        if (userId.equals(email.getUserId())) {
            return;
        }
        ProfileDO profile = profiles.selectById(userId);
        if (profile != null && UserRole.LEADER == UserRole.fromDbValue(profile.getRole())) {
            return;
        }
        throw new BusinessException("FORBIDDEN", "无权删除此邮件");
    }

    @Transactional(rollbackFor = Exception.class)
    public EmailDto reclassify(UUID userId, UUID emailId) {
        EmailDO email = emails.selectOne(new LambdaQueryWrapper<EmailDO>()
                .eq(EmailDO::getId, emailId)
                .eq(EmailDO::getUserId, userId));
        if (email == null) {
            throw new BusinessException("NOT_FOUND", "邮件不存在或已删除");
        }
        if (resolveBody(email).isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "邮件正文为空，无法重新分析");
        }

        GmailAiFallback.GmailAiFields ai =
                classificationService.classify(toGmailMessage(email), email.getDirection(), true);
        applyClassification(email, ai);
        emails.updateById(email);
        return EntityMappers.toEmailDto(email);
    }

    private static void applyClassification(EmailDO email, GmailAiFallback.GmailAiFields ai) {
        email.setAiStageSignal(ai.stageSignal());
        email.setAiPriority(ai.priority());
        email.setAiSummary(ai.summary());
        email.setAiSuggestedAction(ai.suggestedAction());
        email.setAiError(ai.aiError());
        email.setAiProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private static GmailFullMessage toGmailMessage(EmailDO email) {
        return new GmailFullMessage(
                email.getGmailMessageId() == null ? "" : email.getGmailMessageId(),
                email.getGmailThreadId() == null ? "" : email.getGmailThreadId(),
                "",
                email.getFromEmail() == null ? "" : email.getFromEmail(),
                email.getToEmails() == null ? List.of() : email.getToEmails(),
                email.getCcEmails() == null ? List.of() : email.getCcEmails(),
                email.getSubject() == null ? "" : email.getSubject(),
                email.getBodyText() == null ? "" : email.getBodyText(),
                email.getBodyHtml(),
                email.getAttachmentNames() == null ? List.of() : email.getAttachmentNames(),
                Boolean.TRUE.equals(email.getHasAttachments()),
                email.getSentAt() == null ? OffsetDateTime.now(ZoneOffset.UTC) : email.getSentAt(),
                List.of());
    }

    private static String resolveBody(EmailDO email) {
        if (email.getBodyText() != null && !email.getBodyText().isBlank()) {
            return email.getBodyText();
        }
        if (email.getBodyHtml() != null && !email.getBodyHtml().isBlank()) {
            return email.getBodyHtml();
        }
        return "";
    }
}

package com.lovart.maildesk.application.gmail;

import com.lovart.maildesk.application.audit.AuditLogService;
import com.lovart.maildesk.application.dto.SendEmailRequest;
import com.lovart.maildesk.common.enums.ActionType;
import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.domain.email.entity.EmailDO;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.gmail.GmailSentMessage;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.template.entity.EmailTemplateDO;
import com.lovart.maildesk.domain.template.mapper.EmailTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists outbound email rows and KOL / template side effects after a successful Gmail send.
 */
@Service
public class GmailSendPersistenceService {

    private final EmailMapper emails;
    private final KolMapper kols;
    private final EmailTemplateMapper templates;
    private final AuditLogService auditLog;

    public GmailSendPersistenceService(
            EmailMapper emails,
            KolMapper kols,
            EmailTemplateMapper templates,
            AuditLogService auditLog) {
        this.emails = emails;
        this.kols = kols;
        this.templates = templates;
        this.auditLog = auditLog;
    }

    @Transactional(rollbackFor = Exception.class)
    public UUID persistSuccessfulSend(
            UUID userId,
            SendEmailRequest request,
            GmailSentMessage sent,
            String fromEmail,
            List<String> ccEmails) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        EmailDO row = new EmailDO();
        row.setGmailMessageId(sent.messageId());
        row.setGmailThreadId(sent.threadId());
        row.setKolId(request.kolId());
        row.setUserId(userId);
        row.setTemplateId(request.templateId());
        row.setDirection(EmailDirection.OUTBOUND);
        row.setFromEmail(fromEmail == null ? "" : fromEmail);
        row.setToEmails(List.of(request.to().trim()));
        row.setCcEmails(ccEmails);
        row.setSubject(request.subject().trim());
        row.setBodyText(request.englishBody());
        row.setBodyHtml(request.englishBodyHtml());
        row.setBodyZh(request.chineseDraft());
        row.setAttachmentNames(List.of());
        row.setHasAttachments(false);
        row.setSentAt(now);
        row.setAiStageSignal(KolStage.OUTREACH);
        row.setAiPriority("medium");
        row.setAiSummary("人工确认后发送的英文邮件。");
        row.setAiSuggestedAction("等待对方回复。");
        row.setAiProcessedAt(now);
        row.setIsRead(true);
        row.setReadAt(now);
        emails.insert(row);

        KolDO kolPatch = new KolDO();
        kolPatch.setId(request.kolId());
        kolPatch.setLastOutboundAt(now);
        kolPatch.setOwnerUserId(userId);
        kols.updateById(kolPatch);

        if (request.templateId() != null) {
            EmailTemplateDO template = templates.selectById(request.templateId());
            if (template != null) {
                EmailTemplateDO templatePatch = new EmailTemplateDO();
                templatePatch.setId(template.getId());
                int nextCount = (template.getUsedCount() == null ? 0 : template.getUsedCount()) + 1;
                templatePatch.setUsedCount(nextCount);
                templatePatch.setLastUsedAt(now);
                templates.updateById(templatePatch);

                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("kol_id", request.kolId());
                metadata.put("email_record_id", row.getId());
                metadata.put("gmail_message_id", sent.messageId());
                auditLog.append(ActionType.TEMPLATE_USED, "template", request.templateId(), metadata);
            }
        }

        return row.getId();
    }
}

package com.lovart.maildesk.application.gmail;

import com.lovart.maildesk.application.audit.AuditLogService;
import com.lovart.maildesk.application.dto.SendEmailRequest;
import com.lovart.maildesk.application.dto.SendEmailResultDto;
import com.lovart.maildesk.common.enums.ActionType;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.common.exception.GmailIntegrationException;
import com.lovart.maildesk.domain.credential.GoogleCredentialPort;
import com.lovart.maildesk.domain.gmail.GmailClient;
import com.lovart.maildesk.domain.gmail.GmailOutboundMessage;
import com.lovart.maildesk.domain.gmail.GmailSentMessage;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import com.lovart.maildesk.integration.gmail.GmailProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Executes a single Gmail send in its own transaction (used by single-send and batch-send).
 */
@Service
public class GmailSendExecutor {

    private static final int MAX_CC = 20;

    private final KolMapper kols;
    private final ProfileMapper profiles;
    private final GoogleCredentialPort credentials;
    private final GmailClient gmailClient;
    private final GmailProperties gmailProperties;
    private final GmailSendPersistenceService persistence;
    private final AuditLogService auditLog;

    public GmailSendExecutor(
            KolMapper kols,
            ProfileMapper profiles,
            GoogleCredentialPort credentials,
            GmailClient gmailClient,
            GmailProperties gmailProperties,
            GmailSendPersistenceService persistence,
            AuditLogService auditLog) {
        this.kols = kols;
        this.profiles = profiles;
        this.credentials = credentials;
        this.gmailClient = gmailClient;
        this.gmailProperties = gmailProperties;
        this.persistence = persistence;
        this.auditLog = auditLog;
    }

    @Transactional(rollbackFor = Exception.class)
    public SendEmailResultDto execute(UUID userId, SendEmailRequest request) {
        if (!Boolean.TRUE.equals(request.reviewed())) {
            throw new BusinessException("VALIDATION_ERROR", "发送前必须确认已人工审核");
        }

        KolDO kol = kols.selectById(request.kolId());
        if (kol == null) {
            throw new BusinessException("NOT_FOUND", "达人不存在");
        }

        List<String> ccEmails = normalizeCc(request.ccEmails());
        GmailOutboundMessage outbound = new GmailOutboundMessage(
                request.to().trim(),
                ccEmails,
                request.subject().trim(),
                request.englishBody(),
                request.englishBodyHtml());

        if (!gmailProperties.oauthConfigured() || !credentials.hasCredential(userId)) {
            SendEmailResultDto result = new SendEmailResultDto(
                    "not_configured",
                    null,
                    "Gmail 发信配置或当前用户 refresh token 缺失，当前不会发送真实邮件。");
            auditSend(request, result, Map.of("reason", "missing_gmail_configuration"));
            return result;
        }
        if (!credentials.hasGmailSendScope(userId)) {
            throw new GmailIntegrationException(
                    "缺少 Gmail 发送权限，请点击顶栏「重新授权 Gmail」并勾选发送邮件权限。", true);
        }

        try {
            String accessToken = credentials.resolveAccessToken(userId)
                    .orElseThrow(() -> new GmailIntegrationException("未找到 Gmail 授权信息", true))
                    .accessToken();
            GmailSentMessage sent = gmailClient.sendMessage(accessToken, outbound);
            UUID emailRecordId = persistence.persistSuccessfulSend(
                    userId, request, sent, resolveFromEmail(userId), ccEmails);
            SendEmailResultDto result = new SendEmailResultDto(
                    "sent",
                    sent.messageId(),
                    "英文稿已通过 Gmail 发送。");
            Map<String, Object> sentMetadata = new LinkedHashMap<>();
            sentMetadata.put("gmail_message_id", sent.messageId());
            sentMetadata.put("gmail_thread_id", sent.threadId());
            if (emailRecordId != null) {
                sentMetadata.put("email_record_id", emailRecordId);
            }
            auditSend(request, result, sentMetadata);
            return result;
        } catch (GmailIntegrationException ex) {
            if (ex.credentialExpired()) {
                throw ex;
            }
            SendEmailResultDto result = new SendEmailResultDto("failed", null, ex.getMessage());
            auditSend(request, result, Map.of("error", ex.getMessage()));
            return result;
        }
    }

    private String resolveFromEmail(UUID userId) {
        ProfileDO profile = profiles.selectById(userId);
        if (profile == null || profile.getEmail() == null || profile.getEmail().isBlank()) {
            return "";
        }
        return profile.getEmail();
    }

    static List<String> normalizeCc(List<String> ccEmails) {
        if (ccEmails == null || ccEmails.isEmpty()) {
            return List.of();
        }
        if (ccEmails.size() > MAX_CC) {
            throw new BusinessException("VALIDATION_ERROR", "CC 邮箱不能超过 " + MAX_CC + " 个");
        }
        return ccEmails.stream().map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private void auditSend(SendEmailRequest request, SendEmailResultDto result, Map<String, ?> extra) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("to", request.to());
        metadata.put("cc", request.ccEmails() == null ? List.of() : request.ccEmails());
        metadata.put("subject", request.subject());
        metadata.put("template_id", request.templateId());
        metadata.put("status", result.status());
        if (result.messageId() != null) {
            metadata.put("message_id", result.messageId());
        }
        if (extra != null) {
            metadata.putAll(extra);
        }
        auditLog.append(ActionType.EMAIL_SENT, "kol", request.kolId(), metadata);
    }
}

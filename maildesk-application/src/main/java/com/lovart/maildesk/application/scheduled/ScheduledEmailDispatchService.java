package com.lovart.maildesk.application.scheduled;

import com.lovart.maildesk.application.dto.SendEmailRequest;
import com.lovart.maildesk.application.dto.SendEmailResultDto;
import com.lovart.maildesk.application.gmail.GmailSendExecutor;
import com.lovart.maildesk.common.context.UserContext;
import com.lovart.maildesk.common.enums.ScheduledEmailStatus;
import com.lovart.maildesk.common.exception.GmailIntegrationException;
import com.lovart.maildesk.domain.scheduled.ScheduledEmailRetryBackoff;
import com.lovart.maildesk.domain.scheduled.entity.ScheduledEmailDO;
import com.lovart.maildesk.domain.scheduled.mapper.ScheduledEmailMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Claims due {@code scheduled_emails} rows and sends them via {@link GmailSendExecutor}.
 */
@Service
public class ScheduledEmailDispatchService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledEmailDispatchService.class);

    private final ScheduledEmailMapper scheduledEmails;
    private final GmailSendExecutor gmailSendExecutor;
    private final ScheduledEmailDispatchService self;

    public ScheduledEmailDispatchService(
            ScheduledEmailMapper scheduledEmails,
            GmailSendExecutor gmailSendExecutor,
            @Lazy ScheduledEmailDispatchService self) {
        this.scheduledEmails = scheduledEmails;
        this.gmailSendExecutor = gmailSendExecutor;
        this.self = self;
    }

    /**
     * Claim due rows without sending (caller invokes {@link #dispatchClaimed} per row).
     */
    public List<ScheduledEmailDO> claimDue(UUID tenantId, int batchSize) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int limit = Math.max(1, Math.min(batchSize, 50));
        return scheduledEmails.claimDueBatch(now, limit, tenantId);
    }

    /**
     * Send one already-claimed row and update its terminal status.
     */
    @Transactional(rollbackFor = Exception.class)
    public ScheduledEmailDispatchResult.ItemResult dispatchClaimed(ScheduledEmailDO row) {
        UUID actorId = row.getUserId();
        UserContext.setUserId(actorId);
        try {
            SendEmailRequest request = toSendRequest(row);
            SendEmailResultDto result = gmailSendExecutor.execute(actorId, request);
            if ("sent".equals(result.status())) {
                markSent(row.getId(), result.messageId(), OffsetDateTime.now(ZoneOffset.UTC));
                return new ScheduledEmailDispatchResult.ItemResult(row.getId(), "sent", result.message());
            }
            String message = result.message() != null ? result.message() : result.status();
            markFailed(row, message, OffsetDateTime.now(ZoneOffset.UTC));
            return new ScheduledEmailDispatchResult.ItemResult(row.getId(), "failed", message);
        } catch (GmailIntegrationException ex) {
            if (ex.credentialExpired()) {
                throw ex;
            }
            markFailed(row, ex.getMessage(), OffsetDateTime.now(ZoneOffset.UTC));
            return new ScheduledEmailDispatchResult.ItemResult(row.getId(), "failed", ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn("Scheduled email dispatch failed for {}", row.getId(), ex);
            markFailed(row, ex.getMessage(), OffsetDateTime.now(ZoneOffset.UTC));
            return new ScheduledEmailDispatchResult.ItemResult(
                    row.getId(), "failed", ex.getMessage() != null ? ex.getMessage() : "dispatch_error");
        } finally {
            UserContext.clear();
        }
    }

    public ScheduledEmailDispatchResult dispatchDue(UUID tenantId, int batchSize) {
        List<ScheduledEmailDO> claimed = claimDue(tenantId, batchSize);
        int sent = 0;
        int failed = 0;
        List<ScheduledEmailDispatchResult.ItemResult> items = new ArrayList<>();
        for (ScheduledEmailDO row : claimed) {
            ScheduledEmailDispatchResult.ItemResult item = self.dispatchClaimed(row);
            items.add(item);
            if ("sent".equals(item.status())) {
                sent++;
            } else if ("failed".equals(item.status())) {
                failed++;
            }
        }
        return new ScheduledEmailDispatchResult(claimed.size(), sent, failed, 0, items);
    }

    private void markSent(UUID id, String gmailMessageId, OffsetDateTime sentAt) {
        ScheduledEmailDO patch = new ScheduledEmailDO();
        patch.setId(id);
        patch.setStatus(ScheduledEmailStatus.SENT.dbValue());
        patch.setGmailMessageId(gmailMessageId);
        patch.setSentAt(sentAt);
        patch.setLastAttemptAt(sentAt);
        patch.setError(null);
        scheduledEmails.updateById(patch);
    }

    private void markFailed(ScheduledEmailDO row, String error, OffsetDateTime attemptedAt) {
        int attempts = row.getAttemptCount() != null ? row.getAttemptCount() : 0;
        ScheduledEmailDO patch = new ScheduledEmailDO();
        patch.setId(row.getId());
        patch.setStatus(ScheduledEmailStatus.FAILED.dbValue());
        patch.setError(error);
        patch.setLastAttemptAt(attemptedAt);
        scheduledEmails.updateById(patch);
        if (ScheduledEmailRetryBackoff.isTerminal(attempts)) {
            log.warn(
                    "Scheduled email {} reached terminal failure after {} attempts: {}",
                    row.getId(),
                    attempts,
                    error);
        }
    }

    private static SendEmailRequest toSendRequest(ScheduledEmailDO row) {
        return new SendEmailRequest(
                row.getKolId(),
                row.getToEmail(),
                row.getCcEmails() == null ? List.of() : row.getCcEmails(),
                row.getSubject(),
                row.getEnglishBody(),
                row.getEnglishBodyHtml(),
                row.getChineseDraft(),
                row.getTemplateId(),
                true);
    }
}

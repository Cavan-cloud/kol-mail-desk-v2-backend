package com.lovart.maildesk.application.sync.gmail;

import com.lovart.maildesk.application.audit.AuditLogService;
import com.lovart.maildesk.application.dto.GmailSyncStatusDto;
import com.lovart.maildesk.common.enums.ActionType;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.common.exception.GmailIntegrationException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class GmailSyncApplicationService {

    private final GmailSyncService syncService;
    private final AuditLogService auditLog;
    private final Map<UUID, AtomicBoolean> runningByUser = new ConcurrentHashMap<>();
    private final Map<UUID, GmailSyncStatusDto> lastStatusByUser = new ConcurrentHashMap<>();

    public GmailSyncApplicationService(GmailSyncService syncService, AuditLogService auditLog) {
        this.syncService = syncService;
        this.auditLog = auditLog;
    }

    public GmailSyncStatusDto triggerSync(UUID userId, GmailSyncOptions options) {
        AtomicBoolean running = runningByUser.computeIfAbsent(userId, id -> new AtomicBoolean(false));
        if (!running.compareAndSet(false, true)) {
            throw new BusinessException("CONFLICT", "Gmail 同步正在进行中");
        }
        lastStatusByUser.put(userId, GmailSyncStatusDto.startRunning(options.mode()));
        auditLog.append(
                ActionType.SYNC_STARTED,
                "sync",
                userId,
                Map.of("provider", "gmail", "mode", options.mode()));
        try {
            GmailSyncResult result = syncService.sync(userId, options);
            GmailSyncStatusDto status = mapResult(result);
            lastStatusByUser.put(userId, status);
            return status;
        } catch (GmailIntegrationException ex) {
            recordSyncFailed(userId, options.mode(), ex.getMessage());
            GmailSyncStatusDto failed = new GmailSyncStatusDto(
                    false,
                    options.mode(),
                    0,
                    null,
                    null,
                    ex.getMessage());
            lastStatusByUser.put(userId, failed);
            throw ex;
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null ? "Gmail 同步失败" : ex.getMessage();
            recordSyncFailed(userId, options.mode(), message);
            GmailSyncStatusDto failed = new GmailSyncStatusDto(
                    false,
                    options.mode(),
                    0,
                    null,
                    null,
                    message);
            lastStatusByUser.put(userId, failed);
            throw ex;
        } finally {
            running.set(false);
        }
    }

    private void recordSyncFailed(UUID userId, String mode, String message) {
        auditLog.append(
                ActionType.SYNC_FAILED,
                "sync",
                userId,
                Map.of("provider", "gmail", "mode", mode, "message", message == null ? "" : message));
    }

    public GmailSyncStatusDto getStatus(UUID userId) {
        GmailSyncStatusDto status = lastStatusByUser.getOrDefault(userId, GmailSyncStatusDto.idle());
        AtomicBoolean running = runningByUser.get(userId);
        if (running != null && running.get()) {
            return status.withRunning(true);
        }
        return status;
    }

    private static GmailSyncStatusDto mapResult(GmailSyncResult result) {
        if ("not_configured".equals(result.status())) {
            return new GmailSyncStatusDto(false, result.mode(), 0, null, null, result.message());
        }
        return new GmailSyncStatusDto(
                false,
                result.mode(),
                result.processed(),
                result.nextPageToken(),
                OffsetDateTime.now(ZoneOffset.UTC),
                null);
    }
}

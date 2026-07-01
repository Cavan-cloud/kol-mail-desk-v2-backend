package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.application.dto.FeishuSyncStatusDto;
import com.lovart.maildesk.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs {@link FeishuSyncService} and tracks last progress for {@code GET /sync/feishu/status}.
 */
@Service
public class FeishuSyncApplicationService {

    private final FeishuSyncService syncService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<FeishuSyncStatusDto> lastStatus =
            new AtomicReference<>(FeishuSyncStatusDto.idle());

    public FeishuSyncApplicationService(FeishuSyncService syncService) {
        this.syncService = syncService;
    }

    public FeishuSyncStatusDto triggerSync() {
        if (!running.compareAndSet(false, true)) {
            throw new BusinessException("CONFLICT", "飞书同步正在进行中");
        }
        lastStatus.set(FeishuSyncStatusDto.startRunning());
        try {
            FeishuSyncResult result = syncService.sync(FeishuSyncOptions.defaults());
            FeishuSyncStatusDto status = mapResult(result);
            lastStatus.set(status);
            return status;
        } catch (RuntimeException ex) {
            FeishuSyncStatusDto failed = new FeishuSyncStatusDto(
                    false,
                    0,
                    null,
                    ex.getMessage() == null ? "飞书同步失败" : ex.getMessage());
            lastStatus.set(failed);
            throw ex;
        } finally {
            running.set(false);
        }
    }

    public FeishuSyncStatusDto getStatus() {
        FeishuSyncStatusDto status = lastStatus.get();
        if (running.get()) {
            return status.withRunning(true);
        }
        return status;
    }

    private static FeishuSyncStatusDto mapResult(FeishuSyncResult result) {
        if ("not_configured".equals(result.status())) {
            return new FeishuSyncStatusDto(false, 0, null, result.message());
        }
        return new FeishuSyncStatusDto(
                false,
                result.upserted(),
                OffsetDateTime.now(ZoneOffset.UTC),
                null);
    }
}

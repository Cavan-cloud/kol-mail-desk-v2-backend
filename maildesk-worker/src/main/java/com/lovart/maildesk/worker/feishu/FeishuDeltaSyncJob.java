package com.lovart.maildesk.worker.feishu;

import com.lovart.maildesk.application.sync.feishu.FeishuSyncOptions;
import com.lovart.maildesk.application.sync.feishu.FeishuSyncResult;
import com.lovart.maildesk.application.sync.feishu.FeishuSyncService;
import com.lovart.maildesk.common.context.TenantContext;
import com.lovart.maildesk.infrastructure.redis.RedisDistributedLock;
import com.lovart.maildesk.worker.config.WorkerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Scheduled read-only Feishu delta sync — small batch (50 rows) every 30 minutes.
 * Uses a Redis lock so only one worker instance runs at a time.
 */
@Component
@Profile("!backfill")
@ConditionalOnProperty(
        prefix = "maildesk.worker.feishu-delta-sync",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class FeishuDeltaSyncJob {

    private static final Logger log = LoggerFactory.getLogger(FeishuDeltaSyncJob.class);
    static final String LOCK_KEY = FeishuSyncLockKeys.SYNC;

    private final FeishuSyncService syncService;
    private final RedisDistributedLock lock;
    private final WorkerProperties workerProperties;
    private final UUID defaultTenantId;

    public FeishuDeltaSyncJob(
            FeishuSyncService syncService,
            RedisDistributedLock lock,
            WorkerProperties workerProperties,
            @Value("${maildesk.default-tenant-id:00000000-0000-0000-0000-000000000001}") String defaultTenantId) {
        this.syncService = syncService;
        this.lock = lock;
        this.workerProperties = workerProperties;
        this.defaultTenantId = UUID.fromString(defaultTenantId.trim());
    }

    @Scheduled(cron = "${maildesk.worker.feishu-delta-sync.cron:0 */30 * * * *}")
    public void runScheduled() {
        WorkerProperties.FeishuDeltaSync config = workerProperties.feishuDeltaSync();
        Optional<RedisDistributedLock.Handle> handle =
                lock.tryAcquire(LOCK_KEY, config.lockTtl());
        if (handle.isEmpty()) {
            log.debug("Feishu delta sync skipped — lock held by another instance");
            return;
        }

        try {
            TenantContext.setTenantId(defaultTenantId);
            FeishuSyncResult result =
                    syncService.sync(FeishuSyncOptions.deltaBatch(config.maxRecords()));
            if ("not_configured".equals(result.status())) {
                log.info("Feishu delta sync skipped — Feishu credentials not configured");
                return;
            }
            log.info(
                    "Feishu delta sync finished: upserted={}, mergedPairs={}, scannedRows={}",
                    result.upserted(),
                    result.mergedPairs(),
                    result.scannedRows());
        } catch (RuntimeException ex) {
            log.error("Feishu delta sync failed", ex);
        } finally {
            TenantContext.clear();
            lock.release(handle.get());
        }
    }
}

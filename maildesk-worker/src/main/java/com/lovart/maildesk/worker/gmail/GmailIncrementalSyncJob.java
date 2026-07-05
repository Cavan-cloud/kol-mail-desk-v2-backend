package com.lovart.maildesk.worker.gmail;

import com.lovart.maildesk.application.sync.gmail.GmailSyncOptions;
import com.lovart.maildesk.application.sync.gmail.GmailSyncResult;
import com.lovart.maildesk.application.sync.gmail.GmailSyncService;
import com.lovart.maildesk.common.context.TenantContext;
import com.lovart.maildesk.common.context.UserContext;
import com.lovart.maildesk.infrastructure.redis.RedisDistributedLock;
import com.lovart.maildesk.worker.config.WorkerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Scheduled Gmail incremental sync for all users with Google credentials.
 * Runs every 5 minutes by default.
 */
@Component
@Profile("!backfill")
@ConditionalOnProperty(
        prefix = "maildesk.worker.gmail-incremental-sync",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class GmailIncrementalSyncJob {

    private static final Logger log = LoggerFactory.getLogger(GmailIncrementalSyncJob.class);

    private final GmailSyncService syncService;
    private final RedisDistributedLock lock;
    private final WorkerProperties workerProperties;
    private final UUID defaultTenantId;

    public GmailIncrementalSyncJob(
            GmailSyncService syncService,
            RedisDistributedLock lock,
            WorkerProperties workerProperties,
            @Value("${maildesk.default-tenant-id:00000000-0000-0000-0000-000000000001}") String defaultTenantId) {
        this.syncService = syncService;
        this.lock = lock;
        this.workerProperties = workerProperties;
        this.defaultTenantId = UUID.fromString(defaultTenantId.trim());
    }

    @Scheduled(cron = "${maildesk.worker.gmail-incremental-sync.cron:0 */5 * * * *}")
    public void runScheduled() {
        WorkerProperties.GmailIncrementalSync config = workerProperties.gmailIncrementalSync();
        Optional<RedisDistributedLock.Handle> handle =
                lock.tryAcquire(GmailSyncLockKeys.INCREMENTAL, config.lockTtl());
        if (handle.isEmpty()) {
            log.debug("Gmail incremental sync skipped — lock held by another instance");
            return;
        }

        try {
            TenantContext.setTenantId(defaultTenantId);
            List<UUID> userIds = syncService.listUsersDueForIncrementalSync(config.maxUsersPerRun());
            if (userIds.isEmpty()) {
                log.debug("Gmail incremental sync skipped — no users with Google credentials");
                return;
            }
            int synced = 0;
            for (UUID userId : userIds) {
                try {
                    UserContext.setUserId(userId);
                    GmailSyncResult result = syncService.sync(userId, GmailSyncOptions.incremental());
                    if ("synced".equals(result.status())) {
                        synced++;
                        log.info(
                                "Gmail incremental sync user={} processed={} inserted={}",
                                userId,
                                result.processed(),
                                result.insertedEmails());
                    } else {
                        log.debug("Gmail incremental sync user={} skipped: {}", userId, result.message());
                    }
                } catch (RuntimeException ex) {
                    log.warn("Gmail incremental sync failed for user {}: {}", userId, ex.getMessage());
                } finally {
                    UserContext.clear();
                }
            }
            log.info("Gmail incremental sync finished for {} / {} users", synced, userIds.size());
        } finally {
            TenantContext.clear();
            lock.release(handle.get());
        }
    }
}

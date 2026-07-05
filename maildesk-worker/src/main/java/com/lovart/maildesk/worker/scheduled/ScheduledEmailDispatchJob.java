package com.lovart.maildesk.worker.scheduled;

import com.lovart.maildesk.application.scheduled.ScheduledEmailDispatchResult;
import com.lovart.maildesk.application.scheduled.ScheduledEmailDispatchService;
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
 * Dispatches due {@code scheduled_emails} every minute via atomic DB claim
 * ({@code FOR UPDATE SKIP LOCKED}) + {@link GmailSendExecutor}.
 */
@Component
@Profile("!backfill")
@ConditionalOnProperty(
        prefix = "maildesk.worker.scheduled-email-dispatch",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ScheduledEmailDispatchJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduledEmailDispatchJob.class);

    private final ScheduledEmailDispatchService dispatchService;
    private final RedisDistributedLock lock;
    private final WorkerProperties workerProperties;
    private final UUID defaultTenantId;

    public ScheduledEmailDispatchJob(
            ScheduledEmailDispatchService dispatchService,
            RedisDistributedLock lock,
            WorkerProperties workerProperties,
            @Value("${maildesk.default-tenant-id:00000000-0000-0000-0000-000000000001}") String defaultTenantId) {
        this.dispatchService = dispatchService;
        this.lock = lock;
        this.workerProperties = workerProperties;
        this.defaultTenantId = UUID.fromString(defaultTenantId.trim());
    }

    @Scheduled(cron = "${maildesk.worker.scheduled-email-dispatch.cron:0 * * * * *}")
    public void runScheduled() {
        WorkerProperties.ScheduledEmailDispatch config = workerProperties.scheduledEmailDispatch();
        Optional<RedisDistributedLock.Handle> handle =
                lock.tryAcquire(ScheduledEmailDispatchLockKeys.DISPATCH, config.lockTtl());
        if (handle.isEmpty()) {
            log.debug("Scheduled email dispatch skipped — lock held by another instance");
            return;
        }

        try {
            TenantContext.setTenantId(defaultTenantId);
            ScheduledEmailDispatchResult result =
                    dispatchService.dispatchDue(defaultTenantId, config.batchSize());
            if (result.claimed() > 0) {
                log.info(
                        "Scheduled email dispatch finished: claimed={}, sent={}, failed={}",
                        result.claimed(),
                        result.sent(),
                        result.failed());
            }
        } catch (RuntimeException ex) {
            log.error("Scheduled email dispatch tick failed", ex);
        } finally {
            TenantContext.clear();
            lock.release(handle.get());
        }
    }
}

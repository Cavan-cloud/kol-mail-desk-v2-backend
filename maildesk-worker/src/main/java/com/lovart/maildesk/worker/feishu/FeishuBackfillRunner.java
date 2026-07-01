package com.lovart.maildesk.worker.feishu;

import com.lovart.maildesk.application.sync.feishu.FeishuSyncOptions;
import com.lovart.maildesk.application.sync.feishu.FeishuSyncResult;
import com.lovart.maildesk.application.sync.feishu.FeishuSyncService;
import com.lovart.maildesk.common.context.TenantContext;
import com.lovart.maildesk.infrastructure.redis.RedisDistributedLock;
import com.lovart.maildesk.worker.config.BackfillProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * One-shot Feishu full roster backfill for ops / first import.
 * <p>
 * Usage:
 * {@code mvn -pl maildesk-worker spring-boot:run -Dspring-boot.run.profiles=backfill}
 */
@Component
@Profile("backfill")
public class FeishuBackfillRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FeishuBackfillRunner.class);

    private final FeishuSyncService syncService;
    private final RedisDistributedLock lock;
    private final BackfillProperties backfillProperties;
    private final ConfigurableApplicationContext context;
    private final UUID defaultTenantId;

    public FeishuBackfillRunner(
            FeishuSyncService syncService,
            RedisDistributedLock lock,
            BackfillProperties backfillProperties,
            ConfigurableApplicationContext context,
            @Value("${maildesk.default-tenant-id:00000000-0000-0000-0000-000000000001}") String defaultTenantId) {
        this.syncService = syncService;
        this.lock = lock;
        this.backfillProperties = backfillProperties;
        this.context = context;
        this.defaultTenantId = UUID.fromString(defaultTenantId.trim());
    }

    @Override
    public void run(String... args) {
        log.info(
                "FeishuBackfillRunner starting — dryRun={} recentMonths={} exitAfter={}",
                backfillProperties.dryRun(),
                backfillProperties.recentMonths(),
                backfillProperties.exitAfter());

        Optional<RedisDistributedLock.Handle> handle =
                lock.tryAcquire(FeishuSyncLockKeys.SYNC, backfillProperties.lockTtl());
        if (handle.isEmpty()) {
            log.error("Feishu backfill aborted — another Feishu sync holds lock {}", FeishuSyncLockKeys.SYNC);
            exit(1);
            return;
        }

        int exitCode = 0;
        try {
            TenantContext.setTenantId(defaultTenantId);
            FeishuSyncOptions options = FeishuSyncOptions.backfill(
                    backfillProperties.recentMonths(), backfillProperties.dryRun());
            FeishuSyncResult result = syncService.sync(options);

            if ("not_configured".equals(result.status())) {
                log.error("Feishu backfill aborted — {}", result.message());
                exitCode = 1;
            } else {
                log.info(
                        "Feishu backfill finished: dryRun={} sheets={} scannedRows={} mergedPairs={} upserted={} skipped={} ownerMatched={}",
                        result.dryRun(),
                        result.sheetsScanned(),
                        result.scannedRows(),
                        result.mergedPairs(),
                        result.upserted(),
                        result.skipped(),
                        result.ownerMatched());
            }
        } catch (RuntimeException ex) {
            log.error("Feishu backfill failed", ex);
            exitCode = 1;
        } finally {
            TenantContext.clear();
            lock.release(handle.get());
            if (backfillProperties.exitAfter()) {
                exit(exitCode);
            }
        }
    }

    private void exit(int code) {
        int springCode = SpringApplication.exit(context, () -> code);
        System.exit(springCode);
    }
}

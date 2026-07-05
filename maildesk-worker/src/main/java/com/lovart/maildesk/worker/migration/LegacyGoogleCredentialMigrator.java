package com.lovart.maildesk.worker.migration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.common.context.TenantContext;
import com.lovart.maildesk.domain.credential.entity.IntegrationCredentialDO;
import com.lovart.maildesk.domain.credential.mapper.IntegrationCredentialMapper;
import com.lovart.maildesk.domain.crypto.TokenEncryptionPort;
import com.lovart.maildesk.worker.config.MigrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reads plaintext Google tokens from legacy {@code profiles} and writes AES-encrypted
 * rows into {@code integration_credentials} (P6-T10).
 */
@Component
@Profile("migration")
public class LegacyGoogleCredentialMigrator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyGoogleCredentialMigrator.class);
    private static final String CREDENTIAL_TYPE = "google";

    private final MigrationProperties migrationProperties;
    private final IntegrationCredentialMapper credentials;
    private final TokenEncryptionPort encryption;
    private final ObjectMapper mapper;
    private final ConfigurableApplicationContext applicationContext;
    private final UUID defaultTenantId;

    public LegacyGoogleCredentialMigrator(
            MigrationProperties migrationProperties,
            IntegrationCredentialMapper credentials,
            TokenEncryptionPort encryption,
            ObjectMapper mapper,
            ConfigurableApplicationContext applicationContext,
            @Value("${maildesk.default-tenant-id:00000000-0000-0000-0000-000000000001}") String defaultTenantId) {
        this.migrationProperties = migrationProperties;
        this.credentials = credentials;
        this.encryption = encryption;
        this.mapper = mapper.copy().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.applicationContext = applicationContext;
        this.defaultTenantId = UUID.fromString(defaultTenantId.trim());
    }

    @Override
    public void run(String... args) throws Exception {
        if (!migrationProperties.isConfigured()) {
            log.error("maildesk.migration.source-jdbc-url is required for credential migration");
            SpringApplication.exit(applicationContext, () -> 1);
            return;
        }

        TenantContext.setTenantId(defaultTenantId);
        int migrated = 0;
        int skipped = 0;

        try (Connection conn = DriverManager.getConnection(
                migrationProperties.sourceJdbcUrl(),
                migrationProperties.sourceUsername(),
                migrationProperties.sourcePassword())) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT id, google_refresh_token, google_access_token, google_token_expires_at
                    FROM public.profiles
                    WHERE google_refresh_token IS NOT NULL AND btrim(google_refresh_token) <> ''
                    """)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID userId = rs.getObject("id", UUID.class);
                        String refresh = rs.getString("google_refresh_token");
                        String access = rs.getString("google_access_token");
                        OffsetDateTime expiresAt = rs.getObject("google_token_expires_at", OffsetDateTime.class);
                        if (userId == null || refresh == null || refresh.isBlank()) {
                            skipped++;
                            continue;
                        }
                        upsertCredential(userId, access, refresh, expiresAt);
                        migrated++;
                    }
                }
            }
        } finally {
            TenantContext.clear();
        }

        log.info("Google credential migration finished: migrated={}, skipped={}", migrated, skipped);
        SpringApplication.exit(applicationContext, () -> 0);
    }

    private void upsertCredential(
            UUID userId, String accessToken, String refreshToken, OffsetDateTime expiresAt) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("token_type", "Bearer");
        if (accessToken != null && !accessToken.isBlank()) {
            payload.put("access_token", accessToken);
        }
        payload.put("refresh_token", refreshToken);
        if (expiresAt != null) {
            payload.put("expires_at", expiresAt.atZoneSameInstant(ZoneOffset.UTC).toInstant().toString());
        }
        byte[] sealed = encryption.encrypt(mapper.writeValueAsString(payload));

        IntegrationCredentialDO existing = credentials.selectOne(
                new LambdaQueryWrapper<IntegrationCredentialDO>()
                        .eq(IntegrationCredentialDO::getUserId, userId)
                        .eq(IntegrationCredentialDO::getType, CREDENTIAL_TYPE));
        if (existing != null) {
            existing.setEncryptedPayload(sealed);
            existing.setExpiresAt(expiresAt);
            credentials.updateById(existing);
            return;
        }
        IntegrationCredentialDO row = new IntegrationCredentialDO();
        row.setTenantId(defaultTenantId);
        row.setUserId(userId);
        row.setType(CREDENTIAL_TYPE);
        row.setEncryptedPayload(sealed);
        row.setExpiresAt(expiresAt);
        credentials.insert(row);
    }
}

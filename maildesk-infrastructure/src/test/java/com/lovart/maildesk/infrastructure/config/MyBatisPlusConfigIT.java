package com.lovart.maildesk.infrastructure.config;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lovart.maildesk.common.context.TenantContext;
import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.common.enums.Platform;
import com.lovart.maildesk.domain.email.entity.EmailDO;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.tenant.entity.TenantDO;
import com.lovart.maildesk.domain.tenant.mapper.TenantMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Boots a {@code postgres:16-alpine} Testcontainer, applies the Flyway migration
 * bundle, starts {@link MyBatisPlusTestApp}, and exercises every concern of
 * {@code MyBatisPlusConfig}:
 *
 * <ol>
 *   <li>Tenant fallback to {@code DEFAULT_TENANT_ID} when no
 *       {@link TenantContext} is bound.</li>
 *   <li>{@link TenantContext} switching — write + read isolation between two
 *       tenants via {@code TenantLineInnerInterceptor}.</li>
 *   <li>Pagination via {@code PaginationInnerInterceptor}.</li>
 *   <li>Optimistic locking via {@code OptimisticLockerInnerInterceptor} +
 *       {@code @Version}.</li>
 *   <li>JSONB round-trip via {@code JsonbTypeHandler}.</li>
 *   <li>{@code TEXT[]} round-trip via {@code StringArrayTypeHandler}.</li>
 *   <li>PG ENUM round-trip via {@code PgEnumTypeHandler} subclasses.</li>
 *   <li>Soft delete via {@code @TableLogic} ({@code deleted_at TIMESTAMPTZ}).</li>
 * </ol>
 *
 * Skipped (not failed) when no Docker daemon is reachable — same policy as
 * {@code FlywayMigrationIT} so {@code mvn verify} stays green on machines
 * without Docker, and CI catches the real failure mode.
 */
class MyBatisPlusConfigIT {

    private static final UUID DEFAULT_TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_TENANT_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static PostgreSQLContainer<?> postgres;
    private static ConfigurableApplicationContext ctx;
    private static KolMapper kolMapper;
    private static EmailMapper emailMapper;
    private static TenantMapper tenantMapper;
    private static DataSource dataSource;
    private static final ObjectMapper JSON = new ObjectMapper();

    @BeforeAll
    static void bootContext() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available; skipping MyBatis-Plus integration test");

        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("maildesk")
                .withUsername("maildesk")
                .withPassword("maildesk_local");
        postgres.start();

        ctx = new SpringApplicationBuilder(MyBatisPlusTestApp.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.datasource.url=" + postgres.getJdbcUrl(),
                        "spring.datasource.username=" + postgres.getUsername(),
                        "spring.datasource.password=" + postgres.getPassword(),
                        "spring.datasource.driver-class-name=org.postgresql.Driver"
                )
                .run();

        kolMapper = ctx.getBean(KolMapper.class);
        emailMapper = ctx.getBean(EmailMapper.class);
        tenantMapper = ctx.getBean(TenantMapper.class);
        dataSource = ctx.getBean(DataSource.class);

        ensureOtherTenantSeeded();
    }

    @AfterAll
    static void shutdown() {
        if (ctx != null) {
            ctx.close();
        }
        if (postgres != null) {
            postgres.stop();
        }
    }

    @BeforeEach
    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    /**
     * Inserts the secondary tenant used by the cross-tenant tests. Done via raw
     * JDBC so we do not have to thread the FK seeding through the test's
     * assertions about MP-generated SQL.
     */
    private static void ensureOtherTenantSeeded() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tenants (id, name, plan) VALUES (?::uuid, ?, ?) "
                             + "ON CONFLICT (id) DO NOTHING")) {
            ps.setString(1, OTHER_TENANT_ID.toString());
            ps.setString(2, "Test Tenant Two");
            ps.setString(3, "test");
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to seed secondary tenant", e);
        }
    }

    // ---------------------------------------------------------------- helpers --

    private static KolDO newKol(String emailLocalPart, String operator) {
        KolDO kol = new KolDO();
        kol.setEmail(emailLocalPart + "@example.com");
        kol.setName("Kol " + emailLocalPart);
        kol.setFeishuOperatorName(operator);
        kol.setStage(KolStage.OUTREACH);
        kol.setSource("manual");
        kol.setReplyResolved(false);
        return kol;
    }

    private static EmailDO newEmail(String messageId,
                                    List<String> toEmails,
                                    JsonNode aiFields,
                                    EmailDirection direction) {
        EmailDO email = new EmailDO();
        email.setGmailMessageId(messageId);
        email.setGmailThreadId("thread-" + messageId);
        email.setFromEmail("sender@example.com");
        email.setToEmails(toEmails);
        email.setSubject("subject-" + messageId);
        email.setAiExtractedFields(aiFields);
        email.setDirection(direction);
        email.setSentAt(OffsetDateTime.now());
        return email;
    }

    private OffsetDateTime rawDeletedAt(UUID kolId) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT deleted_at FROM kols WHERE id = ?::uuid")) {
            ps.setString(1, kolId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                java.sql.Timestamp ts = rs.getTimestamp("deleted_at");
                return ts == null ? null : ts.toInstant().atOffset(java.time.ZoneOffset.UTC);
            }
        }
    }

    private String rawTenantId(UUID kolId) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT tenant_id FROM kols WHERE id = ?::uuid")) {
            ps.setString(1, kolId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    // ------------------------------------------------------------------ tests --

    @Test
    void writeWithoutContextUsesDefaultTenantAndAuditFill() throws Exception {
        KolDO kol = newKol("default-tenant-" + UUID.randomUUID(), "alice");
        int rows = kolMapper.insert(kol);

        assertThat(rows).isEqualTo(1);
        assertThat(kol.getId()).isNotNull();
        assertThat(kol.getCreatedAt()).isNotNull();
        assertThat(kol.getUpdatedAt()).isNotNull();
        assertThat(kol.getCreatedBy()).isNull();
        assertThat(kol.getVersion()).isZero();
        assertThat(kol.getDeletedAt()).isNull();

        assertThat(rawTenantId(kol.getId())).isEqualTo(DEFAULT_TENANT_ID.toString());
    }

    @Test
    void tenantContextSwitchIsolatesReadsAndWrites() throws Exception {
        // Default tenant row.
        KolDO defaultRow = newKol("def-" + UUID.randomUUID(), "alice");
        kolMapper.insert(defaultRow);

        // Switch tenant — both INSERT and SELECT must now bind to OTHER_TENANT.
        UUID otherRowId;
        try {
            TenantContext.setTenantId(OTHER_TENANT_ID);
            KolDO otherRow = newKol("oth-" + UUID.randomUUID(), "bob");
            kolMapper.insert(otherRow);
            otherRowId = otherRow.getId();
            assertThat(rawTenantId(otherRowId)).isEqualTo(OTHER_TENANT_ID.toString());

            assertThat(kolMapper.selectById(otherRowId)).isNotNull();
            assertThat(kolMapper.selectById(defaultRow.getId()))
                    .as("default-tenant row must be invisible while OTHER tenant is active")
                    .isNull();
        } finally {
            TenantContext.clear();
        }

        // Back on the default tenant — we should see our own row but not OTHER's.
        assertThat(kolMapper.selectById(defaultRow.getId())).isNotNull();
        assertThat(kolMapper.selectById(otherRowId))
                .as("OTHER-tenant row must be invisible from the default tenant")
                .isNull();
    }

    @Test
    void paginationInterceptorReturnsRealCount() {
        String operator = "page-" + UUID.randomUUID();
        for (int i = 0; i < 25; i++) {
            kolMapper.insert(newKol("p" + i + "-" + UUID.randomUUID(), operator));
        }

        Page<KolDO> page = Page.of(2, 10);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KolDO>()
                .eq(KolDO::getFeishuOperatorName, operator);

        Page<KolDO> result = kolMapper.selectPage(page, wrapper);

        assertThat(result.getTotal()).isEqualTo(25L);
        assertThat(result.getRecords()).hasSize(10);
        assertThat(result.getPages()).isEqualTo(3L);
    }

    @Test
    void optimisticLockerRejectsStaleVersion() {
        KolDO kol = newKol("ver-" + UUID.randomUUID(), "carol");
        kolMapper.insert(kol);

        KolDO fresh = kolMapper.selectById(kol.getId());
        assertThat(fresh.getVersion()).isZero();
        fresh.setName("v1");
        int first = kolMapper.updateById(fresh);
        assertThat(first).isEqualTo(1);
        assertThat(fresh.getVersion()).isEqualTo(1);

        // Replay the original (stale) row — its in-memory version is still 0.
        KolDO stale = new KolDO();
        stale.setId(kol.getId());
        stale.setName("v2-stale");
        stale.setVersion(0);
        int second = kolMapper.updateById(stale);
        assertThat(second)
                .as("stale-version update must affect 0 rows")
                .isZero();

        // DB still shows v1.
        KolDO latest = kolMapper.selectById(kol.getId());
        assertThat(latest.getName()).isEqualTo("v1");
        assertThat(latest.getVersion()).isEqualTo(1);
    }

    @Test
    void jsonbTypeHandlerRoundTrips() {
        ObjectNode payload = JSON.createObjectNode();
        payload.put("priority", "high");
        payload.put("score", 7);
        payload.putArray("tags").add("urgent").add("vip");

        EmailDO email = newEmail("jsonb-" + UUID.randomUUID(),
                List.of("a@example.com"),
                payload,
                EmailDirection.INBOUND);
        emailMapper.insert(email);

        EmailDO fetched = emailMapper.selectById(email.getId());
        JsonNode loaded = fetched.getAiExtractedFields();
        assertThat(loaded).isNotNull();
        assertThat(loaded.get("priority").asText()).isEqualTo("high");
        assertThat(loaded.get("score").asInt()).isEqualTo(7);
        assertThat(loaded.get("tags").isArray()).isTrue();
        assertThat(loaded.get("tags").size()).isEqualTo(2);
    }

    @Test
    void stringArrayTypeHandlerRoundTrips() {
        List<String> to = List.of("first@example.com", "second@example.com");
        List<String> cc = List.of("cc-one@example.com");

        EmailDO email = newEmail("arr-" + UUID.randomUUID(),
                to,
                JSON.createObjectNode(),
                EmailDirection.OUTBOUND);
        email.setCcEmails(cc);
        emailMapper.insert(email);

        EmailDO fetched = emailMapper.selectById(email.getId());
        assertThat(fetched.getToEmails()).containsExactlyElementsOf(to);
        assertThat(fetched.getCcEmails()).containsExactlyElementsOf(cc);
        assertThat(fetched.getDirection()).isEqualTo(EmailDirection.OUTBOUND);
    }

    @Test
    void platformTypeHandlerRoundTrips() {
        KolDO kol = newKol("platform-" + UUID.randomUUID(), "frank");
        kol.setPrimaryPlatform(Platform.TIKTOK);
        kol.setAgreedPlatform(Platform.YOUTUBE);
        kolMapper.insert(kol);

        KolDO fetched = kolMapper.selectById(kol.getId());
        assertThat(fetched.getPrimaryPlatform()).isEqualTo(Platform.TIKTOK);
        assertThat(fetched.getAgreedPlatform()).isEqualTo(Platform.YOUTUBE);

        fetched.setPrimaryPlatform(Platform.INSTAGRAM);
        kolMapper.updateById(fetched);

        KolDO refetched = kolMapper.selectById(kol.getId());
        assertThat(refetched.getPrimaryPlatform()).isEqualTo(Platform.INSTAGRAM);
    }

    @Test
    void pgEnumTypeHandlerRoundTrips() {
        KolDO kol = newKol("enum-" + UUID.randomUUID(), "dan");
        kol.setStage(KolStage.NEGOTIATING);
        kolMapper.insert(kol);

        KolDO fetched = kolMapper.selectById(kol.getId());
        assertThat(fetched.getStage()).isEqualTo(KolStage.NEGOTIATING);

        fetched.setStage(KolStage.PRODUCING);
        kolMapper.updateById(fetched);

        KolDO refetched = kolMapper.selectById(kol.getId());
        assertThat(refetched.getStage()).isEqualTo(KolStage.PRODUCING);
    }

    @Test
    void logicalDeleteHidesRowFromSubsequentReads() throws Exception {
        KolDO kol = newKol("soft-" + UUID.randomUUID(), "eve");
        kolMapper.insert(kol);
        UUID id = kol.getId();

        int deleted = kolMapper.deleteById(id);
        assertThat(deleted).isEqualTo(1);

        assertThat(kolMapper.selectById(id))
                .as("logically deleted row must be invisible to MP reads")
                .isNull();

        assertThat(rawDeletedAt(id))
                .as("DB column deleted_at must be non-null after soft delete")
                .isNotNull();
    }

    @Test
    void tenantsTableIsExemptFromTenantInjection() {
        // tenants has no tenant_id column. If TenantLineInnerInterceptor injected
        // one, this INSERT would fail with a SQL error. Selecting back must also
        // succeed irrespective of which tenant is currently bound.
        TenantDO tenant = new TenantDO();
        tenant.setName("Probe Tenant " + UUID.randomUUID());
        tenant.setPlan("test");
        tenant.setStatus("active");
        tenantMapper.insert(tenant);
        assertThat(tenant.getId()).isNotNull();

        TenantContext.setTenantId(OTHER_TENANT_ID);
        try {
            assertThat(tenantMapper.selectById(tenant.getId())).isNotNull();
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void pgEnumValuesCoverFullKolStageSet() {
        // Sanity check: every KolStage Java constant has a matching PG enum
        // label (i.e. PgEnumTypeHandler#toDbValue is total over the enum).
        Set<String> dbLabels = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT e.enumlabel FROM pg_enum e "
                             + "JOIN pg_type t ON t.oid = e.enumtypid "
                             + "WHERE t.typname = 'kol_stage'")) {
            while (rs.next()) {
                dbLabels.add(rs.getString(1));
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        for (KolStage stage : KolStage.values()) {
            assertThat(dbLabels)
                    .as("kol_stage PG enum must contain label for %s", stage)
                    .contains(stage.name().toLowerCase());
        }
    }
}

-- Dev reset: truncate the business tables in reverse-dependency order so the
-- seed bundle can re-run from a clean slate. Does NOT truncate `tenants`
-- (the dev tenant in V3 must survive) and does NOT touch `flyway_schema_history`.
--
-- ⚠️⚠️⚠️  DO NOT RUN IN PROD. dev-only loader (SeedRunner with
-- maildesk.seed.reset=true, or manual psql against a local DB).
-- The PG role here must be a superuser / table owner for TRUNCATE CASCADE.

TRUNCATE TABLE
    actions,
    ai_usage_log,
    sync_jobs,
    integration_credentials,
    scheduled_emails,
    emails,
    email_threads,
    email_templates,
    kols,
    profiles
RESTART IDENTITY CASCADE;

-- V15: provider + estimated cost columns for multi-vendor AI usage tracking (ADR-007 / P4-T10).

ALTER TABLE ai_usage_log
    ADD COLUMN provider TEXT NOT NULL DEFAULT 'unknown',
    ADD COLUMN estimated_cost_cny NUMERIC(14, 8);

ALTER TABLE ai_usage_log ALTER COLUMN provider DROP DEFAULT;

CREATE INDEX idx_ai_usage_log_tenant_provider_created
    ON ai_usage_log (tenant_id, provider, created_at DESC);

-- V13: ai_usage_log (AI capability usage / cost tracking).
-- New in v2 (design 02-backend-design §4.2).

CREATE TABLE ai_usage_log (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL REFERENCES tenants(id),
    user_id           UUID REFERENCES profiles(id),
    capability        TEXT NOT NULL,
    model             TEXT,
    prompt_tokens     INT,
    completion_tokens INT,
    duration_ms       INT,
    success           BOOLEAN,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID,
    deleted_at        TIMESTAMPTZ,
    version           INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_ai_usage_log_tenant_created ON ai_usage_log (tenant_id, created_at DESC);

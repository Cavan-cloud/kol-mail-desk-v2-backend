-- V12: sync_jobs (Worker job queue / state).
-- New in v2 (design 02-backend-design §4.2).

CREATE TABLE sync_jobs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    user_id       UUID REFERENCES profiles(id),
    type          TEXT NOT NULL,
    payload       JSONB,
    status        TEXT NOT NULL DEFAULT 'pending',
    attempt_count INT NOT NULL DEFAULT 0,
    last_error    TEXT,
    started_at    TIMESTAMPTZ,
    finished_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by    UUID,
    updated_by    UUID,
    deleted_at    TIMESTAMPTZ,
    version       INT NOT NULL DEFAULT 0,
    CONSTRAINT sync_jobs_type_check
        CHECK (type IN ('gmail.incremental', 'gmail.history', 'feishu.delta')),
    CONSTRAINT sync_jobs_status_check
        CHECK (status IN ('pending', 'running', 'done', 'failed'))
);

CREATE INDEX idx_sync_jobs_pending ON sync_jobs (status, created_at) WHERE status = 'pending';

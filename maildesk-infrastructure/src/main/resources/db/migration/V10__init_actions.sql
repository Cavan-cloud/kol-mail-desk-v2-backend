-- V10: actions (append-only audit log).
-- Legacy source: supabase 001.
-- Deviation: legacy columns user_id / action renamed to actor_user_id / action_type
-- to match the v2 audit convention (.cursor/rules backend-java audit section).
-- Audit metadata columns are included for table-convention consistency even though
-- the actions table is logically append-only.

CREATE TABLE actions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID NOT NULL REFERENCES tenants(id),
    actor_user_id  UUID REFERENCES profiles(id),
    action_type    action_type NOT NULL,
    target_type    TEXT NOT NULL,
    target_id      UUID,
    metadata       JSONB DEFAULT '{}'::jsonb,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     UUID,
    updated_by     UUID,
    deleted_at     TIMESTAMPTZ,
    version        INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_actions_actor   ON actions (actor_user_id);
CREATE INDEX idx_actions_target  ON actions (target_type, target_id);
CREATE INDEX idx_actions_created ON actions (created_at DESC);

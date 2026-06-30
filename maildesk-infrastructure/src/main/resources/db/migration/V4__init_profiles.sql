-- V4: profiles (application users; OAuth-authenticated).
-- Legacy source: supabase 001 + 002.
-- Deviations from legacy:
--   * profiles.id no longer references auth.users (no Supabase in v2); it is an
--     independent UUID PK (still the OAuth user identity).
--   * Plaintext google_refresh_token / google_access_token / google_token_expires_at
--     columns are dropped. OAuth tokens now live AES-256 encrypted in
--     integration_credentials (see V11). Per-user Gmail sync cursor columns
--     (last_synced_history_id / last_synced_at) are retained.
--   * Added multi-tenant + audit columns.

CREATE TABLE profiles (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id),
    display_name            TEXT NOT NULL,
    email                   TEXT NOT NULL,
    role                    TEXT NOT NULL DEFAULT 'member',
    status                  TEXT NOT NULL DEFAULT 'pending_approval',
    mentor_user_id          UUID REFERENCES profiles(id),
    feishu_operator_name    TEXT,
    last_synced_history_id  TEXT,
    last_synced_at          TIMESTAMPTZ,
    approved_at             TIMESTAMPTZ,
    approved_by             UUID REFERENCES profiles(id),
    departed_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by              UUID,
    updated_by              UUID,
    deleted_at              TIMESTAMPTZ,
    version                 INT NOT NULL DEFAULT 0,
    CONSTRAINT profiles_role_check   CHECK (role IN ('member', 'leader', 'full_time', 'intern')),
    CONSTRAINT profiles_status_check CHECK (status IN ('pending_approval', 'active', 'departed'))
);

CREATE INDEX idx_profiles_status         ON profiles (status);
CREATE INDEX idx_profiles_mentor         ON profiles (mentor_user_id);
CREATE INDEX idx_profiles_feishu_operator ON profiles (feishu_operator_name);
CREATE INDEX idx_profiles_tenant_email   ON profiles (tenant_id, email);

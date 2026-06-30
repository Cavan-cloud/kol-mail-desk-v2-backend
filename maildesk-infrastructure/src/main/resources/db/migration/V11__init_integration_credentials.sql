-- V11: integration_credentials (encrypted OAuth / API credentials).
-- New in v2 (design 02-backend-design §4.2). Gmail is per-user; Feishu is per-tenant.
-- encrypted_payload holds the AES-256 encrypted JSON; tokens are NEVER stored plaintext.

CREATE TABLE integration_credentials (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL REFERENCES tenants(id),
    user_id           UUID REFERENCES profiles(id),
    type              TEXT NOT NULL,
    encrypted_payload BYTEA NOT NULL,
    expires_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID,
    deleted_at        TIMESTAMPTZ,
    version           INT NOT NULL DEFAULT 0,
    CONSTRAINT integration_credentials_type_check CHECK (type IN ('google', 'feishu', 'kimi'))
);

CREATE INDEX idx_integration_credentials_lookup
    ON integration_credentials (tenant_id, type, user_id);

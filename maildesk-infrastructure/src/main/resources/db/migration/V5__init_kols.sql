-- V5: kols (creators / 达人).
-- Legacy source: supabase 001 + 002 + 003 + 006 + 008 + 009 + 010 (consolidated).
-- Keeps: normalized_email generated column, reply_resolved, source, feishu_* fields,
--        feishu_outreach_at, stage default 'outreach'.
-- Deviation: composite unique now includes tenant_id; added audit columns.

CREATE TABLE kols (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL REFERENCES tenants(id),
    email                 TEXT NOT NULL,
    normalized_email      TEXT GENERATED ALWAYS AS (lower(trim(email))) STORED,
    name                  TEXT,
    handle                TEXT,
    primary_platform      platform,
    stage                 kol_stage NOT NULL DEFAULT 'outreach',
    status                kol_status NOT NULL DEFAULT 'active',
    owner_user_id         UUID REFERENCES profiles(id),
    last_inbound_at       TIMESTAMPTZ,
    last_outbound_at      TIMESTAMPTZ,
    agreed_price          NUMERIC,
    agreed_platform       platform,
    agreed_deadline       DATE,
    notes                 TEXT,
    type                  TEXT,
    external_profile_url  TEXT,
    platform_handle       TEXT,
    source                TEXT NOT NULL DEFAULT 'gmail',
    feishu_record_id      TEXT,
    feishu_table_id       TEXT,
    feishu_operator_name  TEXT NOT NULL DEFAULT '',
    last_feishu_synced_at TIMESTAMPTZ,
    feishu_outreach_at    DATE,
    reply_resolved        BOOLEAN NOT NULL DEFAULT false,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            UUID,
    updated_by            UUID,
    deleted_at            TIMESTAMPTZ,
    version               INT NOT NULL DEFAULT 0,
    CONSTRAINT kols_source_check CHECK (source IN ('gmail', 'feishu', 'manual'))
);

COMMENT ON COLUMN kols.reply_resolved IS
    'Manual override: when true, this KOL is excluded from needs-my-reply signals even if the latest email is inbound. Auto-cleared by Gmail sync on a new inbound message.';
COMMENT ON COLUMN kols.feishu_outreach_at IS
    'Business outreach/contact date imported from Feishu sheet. Used by board time-window filters; NULL means the source row did not expose a reliable date.';

-- Composite uniqueness: same email contacted by different operators = separate ownership rows (legacy 003).
CREATE UNIQUE INDEX kols_email_operator_uidx
    ON kols (tenant_id, normalized_email, feishu_operator_name);

CREATE INDEX idx_kols_owner               ON kols (owner_user_id);
CREATE INDEX idx_kols_owner_created       ON kols (owner_user_id, created_at DESC);
CREATE INDEX idx_kols_stage               ON kols (stage);
CREATE INDEX idx_kols_status              ON kols (status);
CREATE INDEX idx_kols_source              ON kols (source);
CREATE INDEX idx_kols_feishu_record       ON kols (feishu_record_id);
CREATE INDEX idx_kols_feishu_operator_name ON kols (feishu_operator_name);
CREATE INDEX idx_kols_feishu_outreach_at  ON kols (feishu_outreach_at);

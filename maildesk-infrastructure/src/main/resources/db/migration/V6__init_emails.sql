-- V6: emails.
-- Legacy source: supabase 001 + 002 (template_id) + 010 (perf index).
-- template_id FK + index are added in V8 (after email_templates exists).
-- thread_id FK + index are added in V7 (after email_threads exists).
-- Deviation: tenant_id in unique key; added full audit columns (legacy had only created_at).

CREATE TABLE emails (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL REFERENCES tenants(id),
    gmail_message_id     TEXT NOT NULL,
    gmail_thread_id      TEXT NOT NULL,
    kol_id               UUID REFERENCES kols(id),
    user_id              UUID REFERENCES profiles(id),
    template_id          UUID,
    direction            email_direction NOT NULL,
    from_email           TEXT NOT NULL,
    to_emails            TEXT[] NOT NULL,
    cc_emails            TEXT[],
    subject              TEXT,
    body_text            TEXT,
    body_html            TEXT,
    body_zh              TEXT,
    attachment_names     TEXT[] DEFAULT '{}',
    has_attachments      BOOLEAN DEFAULT false,
    sent_at              TIMESTAMPTZ NOT NULL,
    ai_stage_signal      kol_stage,
    ai_priority          TEXT,
    ai_summary           TEXT,
    ai_extracted_fields  JSONB DEFAULT '{}'::jsonb,
    ai_suggested_action  TEXT,
    ai_error             TEXT,
    ai_processed_at      TIMESTAMPTZ,
    is_read              BOOLEAN DEFAULT false,
    read_at              TIMESTAMPTZ,
    read_by              UUID REFERENCES profiles(id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           UUID,
    updated_by           UUID,
    deleted_at           TIMESTAMPTZ,
    version              INT NOT NULL DEFAULT 0,
    CONSTRAINT emails_priority_check CHECK (ai_priority IS NULL OR ai_priority IN ('high', 'medium', 'low')),
    CONSTRAINT emails_message_user_ukey UNIQUE (tenant_id, gmail_message_id, user_id)
);

CREATE INDEX idx_emails_kol       ON emails (kol_id);
CREATE INDEX idx_emails_user      ON emails (user_id);
CREATE INDEX idx_emails_sent_at   ON emails (sent_at DESC);
CREATE INDEX idx_emails_unread    ON emails (is_read) WHERE is_read = false;
CREATE INDEX idx_emails_user_sent ON emails (user_id, sent_at DESC);

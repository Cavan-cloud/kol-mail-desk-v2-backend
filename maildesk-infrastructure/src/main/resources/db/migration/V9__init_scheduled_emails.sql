-- V9: scheduled_emails.
-- Legacy source: supabase 002 + 004 (processing status) + 007 (english_body_html).
-- status check includes the intermediate 'processing' for atomic worker claim.

CREATE TABLE scheduled_emails (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL REFERENCES tenants(id),
    kol_id            UUID REFERENCES kols(id),
    user_id           UUID REFERENCES profiles(id),
    template_id       UUID REFERENCES email_templates(id),
    to_email          TEXT NOT NULL,
    cc_emails         TEXT[] DEFAULT '{}',
    subject           TEXT NOT NULL,
    english_body      TEXT NOT NULL,
    english_body_html TEXT,
    chinese_draft     TEXT,
    scheduled_at      TIMESTAMPTZ NOT NULL,
    status            TEXT NOT NULL DEFAULT 'scheduled',
    attempt_count     INTEGER NOT NULL DEFAULT 0,
    last_attempt_at   TIMESTAMPTZ,
    gmail_message_id  TEXT,
    error             TEXT,
    sent_at           TIMESTAMPTZ,
    cancelled_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID,
    deleted_at        TIMESTAMPTZ,
    version           INT NOT NULL DEFAULT 0,
    CONSTRAINT scheduled_emails_status_check
        CHECK (status IN ('scheduled', 'processing', 'sent', 'cancelled', 'failed'))
);

COMMENT ON COLUMN scheduled_emails.english_body_html IS
    'Optional rich-text (HTML) body. When present the worker sends multipart/alternative with both english_body (plain) and this column (html). NULL => text/plain only.';

CREATE INDEX idx_scheduled_emails_status_time ON scheduled_emails (status, scheduled_at);
CREATE INDEX idx_scheduled_emails_user        ON scheduled_emails (user_id);
CREATE INDEX idx_scheduled_emails_kol         ON scheduled_emails (kol_id);
CREATE INDEX idx_scheduled_emails_template    ON scheduled_emails (template_id);

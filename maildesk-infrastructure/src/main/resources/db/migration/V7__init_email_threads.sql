-- V7: email_threads (thread aggregation).
-- New in v2. Legacy stored gmail_thread_id directly on emails with no thread table.
-- This table aggregates a Gmail conversation so the workbench can show thread-level
-- metadata (subject / last message time / linked KOL) without scanning all emails.

CREATE TABLE email_threads (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    gmail_thread_id TEXT NOT NULL,
    kol_id          UUID REFERENCES kols(id),
    subject         TEXT,
    last_message_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         INT NOT NULL DEFAULT 0,
    CONSTRAINT email_threads_gmail_thread_ukey UNIQUE (tenant_id, gmail_thread_id)
);

CREATE INDEX idx_email_threads_kol             ON email_threads (kol_id);
CREATE INDEX idx_email_threads_last_message_at ON email_threads (last_message_at DESC);

-- Link emails to the aggregated thread row (column added here since email_threads
-- did not exist when the emails table was created in V6).
ALTER TABLE emails
    ADD COLUMN thread_id UUID REFERENCES email_threads(id);

CREATE INDEX idx_emails_thread ON emails (thread_id);

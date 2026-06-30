-- V8: email_templates.
-- Legacy source: supabase 001 + 002 (used_count / last_used_at).
-- Also wires the emails.template_id FK + index (templates now exist).

CREATE TABLE email_templates (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL REFERENCES tenants(id),
    name         TEXT NOT NULL,
    scenario     TEXT,
    subject      TEXT NOT NULL,
    body         TEXT NOT NULL,
    used_count   INTEGER NOT NULL DEFAULT 0,
    last_used_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   UUID,
    updated_by   UUID,
    deleted_at   TIMESTAMPTZ,
    version      INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_email_templates_tenant ON email_templates (tenant_id);

-- Deferred FK from emails.template_id (column created in V6).
ALTER TABLE emails
    ADD CONSTRAINT emails_template_id_fkey
    FOREIGN KEY (template_id) REFERENCES email_templates(id);

CREATE INDEX idx_emails_template ON emails (template_id);

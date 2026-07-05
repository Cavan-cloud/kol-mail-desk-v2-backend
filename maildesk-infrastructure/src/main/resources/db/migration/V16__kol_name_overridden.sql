-- V16: preserve manual workbench renames across Feishu sync.
-- When name_overridden = true, Feishu upsert skips updating kols.name.

ALTER TABLE kols
    ADD COLUMN name_overridden BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN kols.name_overridden IS
    'When true, Feishu sync skips updating name (manual workbench rename).';

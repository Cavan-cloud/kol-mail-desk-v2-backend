-- V17: preserve manual stage calibration across Feishu sync.
-- When stage_override = true, Feishu upsert skips updating kols.stage.

ALTER TABLE kols
    ADD COLUMN stage_override BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN kols.stage_override IS
    'When true, Feishu sync skips updating stage (manual workbench calibration).';

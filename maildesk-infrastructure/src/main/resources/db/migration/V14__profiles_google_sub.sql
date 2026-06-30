-- V14: add google_sub to profiles.
-- google_sub is the stable Google subject identifier and is used as the upsert
-- key on every OAuth login (P1-T04). UNIQUE so a single Google identity maps
-- to exactly one profile per tenant; partial unique because v1 rows imported
-- pre-OAuth may have NULL until first login.

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS google_sub TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS idx_profiles_google_sub
    ON profiles (google_sub)
    WHERE google_sub IS NOT NULL;

-- V1: PostgreSQL extensions.
-- pgcrypto provides gen_random_uuid(), used as the default for every UUID primary key.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

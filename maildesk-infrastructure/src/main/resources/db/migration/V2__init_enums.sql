-- V2: Domain enum types (carried over from legacy supabase migrations 001 + 005).
-- kol_stage keeps ALL legacy values (incl. the legacy-only 'replied') and adds the
-- v3.3 funnel additions 'producing' (制作中) and 'declined' (已拒绝).

CREATE TYPE kol_stage AS ENUM (
    'outreach',     -- 触达
    'replied',      -- legacy value, preserved for backward compatibility
    'negotiating',  -- 沟通/议价
    'confirmed',    -- 确认合作
    'producing',    -- 制作中 (added in legacy 005)
    'reviewing',    -- 审稿/待发布
    'published',    -- 发布
    'paying',       -- 付款
    'reinvest',     -- 复投
    'declined'      -- 已拒绝 (added in legacy 005)
);

CREATE TYPE kol_status AS ENUM (
    'active',
    'unassigned',
    'orphaned',
    'closed'
);

CREATE TYPE platform AS ENUM (
    'tiktok',
    'instagram',
    'youtube',
    'x',
    'other'
);

CREATE TYPE email_direction AS ENUM (
    'inbound',
    'outbound'
);

CREATE TYPE action_type AS ENUM (
    'stage_change',
    'owner_change',
    'email_sent',
    'email_read',
    'kol_claimed',
    'user_approved',
    'user_departed',
    'template_used',
    'sync_started',
    'sync_failed'
);

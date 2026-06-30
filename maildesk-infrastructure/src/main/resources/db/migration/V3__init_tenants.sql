-- V3: Multi-tenancy root table.
-- New in v2 (no equivalent in the legacy single-tenant Supabase schema).
-- The tenants table is itself exempt from the tenant_id column convention.

CREATE TABLE tenants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    plan        TEXT NOT NULL DEFAULT 'lovart_internal',
    status      TEXT NOT NULL DEFAULT 'active',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    updated_by  UUID,
    deleted_at  TIMESTAMPTZ,
    version     INT NOT NULL DEFAULT 0,
    CONSTRAINT tenants_status_check CHECK (status IN ('active', 'suspended', 'closed'))
);

COMMENT ON TABLE tenants IS 'Tenant root for multi-tenant isolation. Application-level isolation via MyBatis TenantLineInnerInterceptor.';

-- Built-in dev/default tenant. Must match DEFAULT_TENANT_ID in .env.example.
INSERT INTO tenants (id, name, plan)
VALUES ('00000000-0000-0000-0000-000000000001', 'Lovart Internal', 'lovart_internal');

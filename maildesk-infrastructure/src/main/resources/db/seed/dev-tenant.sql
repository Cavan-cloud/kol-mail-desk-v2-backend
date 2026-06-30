-- Dev seed: tenant.
-- Re-asserts the built-in dev tenant (already inserted by V3 migration). Kept as
-- a separate seed so the seed bundle is self-contained when someone wipes data
-- via dev-reset.sql (which intentionally does NOT truncate `tenants`).
--
-- DEV-ONLY. Not loaded under the prod profile.

INSERT INTO tenants (id, name, plan, status, version)
VALUES ('00000000-0000-0000-0000-000000000001'::uuid,
        'Lovart Internal',
        'lovart_internal',
        'active',
        0)
ON CONFLICT (id) DO NOTHING;

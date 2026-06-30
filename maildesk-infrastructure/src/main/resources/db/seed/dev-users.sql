-- Dev seed: 4 users (1 leader + 2 members + 1 intern).
-- Stable UUIDs — referenced by dev-kols / dev-emails / dev-templates seeds and by tests.
--
--   leader   00000000-0000-0000-0000-000000000010   王雨     feishu_operator_name='王雨'
--   member1  00000000-0000-0000-0000-000000000011   张瑞     feishu_operator_name='张瑞'
--   member2  00000000-0000-0000-0000-000000000012   李欣     feishu_operator_name='李欣'
--   intern   00000000-0000-0000-0000-000000000013   陈柏     mentor = leader
--
-- DEV-ONLY. Not loaded under prod profile.

INSERT INTO profiles (id, tenant_id, display_name, email, role, status, feishu_operator_name, mentor_user_id, approved_at, version)
VALUES
    ('00000000-0000-0000-0000-000000000010'::uuid,
     '00000000-0000-0000-0000-000000000001'::uuid,
     '王雨', 'wangyu@lovart.dev', 'leader', 'active', '王雨', NULL, now(), 0),
    ('00000000-0000-0000-0000-000000000011'::uuid,
     '00000000-0000-0000-0000-000000000001'::uuid,
     '张瑞', 'zhangrui@lovart.dev', 'member', 'active', '张瑞',
     '00000000-0000-0000-0000-000000000010'::uuid, now(), 0),
    ('00000000-0000-0000-0000-000000000012'::uuid,
     '00000000-0000-0000-0000-000000000001'::uuid,
     '李欣', 'lixin@lovart.dev', 'member', 'active', '李欣',
     '00000000-0000-0000-0000-000000000010'::uuid, now(), 0),
    ('00000000-0000-0000-0000-000000000013'::uuid,
     '00000000-0000-0000-0000-000000000001'::uuid,
     '陈柏', 'chenbai@lovart.dev', 'intern', 'active', NULL,
     '00000000-0000-0000-0000-000000000010'::uuid, now(), 0)
ON CONFLICT (id) DO NOTHING;

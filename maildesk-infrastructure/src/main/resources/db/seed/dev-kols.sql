-- Dev seed: 30 KOLs spanning all 10 kol_stage values, 3 platforms, 3 sources,
-- and 4 owners (leader 10 / member1 10 / member2 5 / pool=NULL 5).
--
-- Stable UUIDs: 11111111-1111-1111-1111-0000000000NN (NN = 01..30).
-- agreed_price filled on ~half. last_inbound_at/last_outbound_at set so 3 rows
-- look "needs my reply" (last_inbound > last_outbound). reply_resolved=true on 5.
-- feishu_outreach_at spread across the last 90 days for board time-window tests.
--
-- DEV-ONLY.

WITH leader   AS (SELECT '00000000-0000-0000-0000-000000000010'::uuid AS id),
     member1  AS (SELECT '00000000-0000-0000-0000-000000000011'::uuid AS id),
     member2  AS (SELECT '00000000-0000-0000-0000-000000000012'::uuid AS id)
INSERT INTO kols (
    id, tenant_id, email, name, handle, primary_platform, stage, status,
    owner_user_id, last_inbound_at, last_outbound_at, agreed_price,
    feishu_operator_name, source, feishu_outreach_at, reply_resolved,
    created_by, updated_by, version
)
SELECT
    seed.id::uuid,
    '00000000-0000-0000-0000-000000000001'::uuid,
    seed.email,
    seed.name,
    seed.handle,
    seed.platform::platform,
    seed.stage::kol_stage,
    'active'::kol_status,
    seed.owner::uuid,
    seed.last_inbound,
    seed.last_outbound,
    seed.price,
    seed.operator,
    seed.source,
    (now() - (seed.days_ago || ' days')::interval)::date,
    seed.reply_resolved,
    '00000000-0000-0000-0000-000000000010'::uuid,
    '00000000-0000-0000-0000-000000000010'::uuid,
    0
FROM (
    VALUES
    -- (id, email, name, handle, platform, stage, owner, last_inbound, last_outbound, price, operator, source, days_ago, reply_resolved)
    ('11111111-1111-1111-1111-000000000001','alice01@example.com','Alice Wong','@alice01','tiktok',  'outreach',    '00000000-0000-0000-0000-000000000010', NULL,                       NULL,                       NULL,  '王雨','feishu',  1,  false),
    ('11111111-1111-1111-1111-000000000002','alice02@example.com','Alice Liu','@alice02','tiktok',  'outreach',    '00000000-0000-0000-0000-000000000010', NULL,                       NULL,                       NULL,  '王雨','feishu',  3,  false),
    ('11111111-1111-1111-1111-000000000003','bob03@example.com','Bob Smith','@bob03','instagram',  'outreach',    '00000000-0000-0000-0000-000000000011', NULL,                       NULL,                       NULL,  '张瑞','feishu',  5,  false),
    ('11111111-1111-1111-1111-000000000004','carol04@example.com','Carol Chen','@carol04','youtube','replied',    '00000000-0000-0000-0000-000000000010', now() - interval '2 days',  now() - interval '5 days',  NULL,  '王雨','feishu',  7,  false),
    ('11111111-1111-1111-1111-000000000005','dan05@example.com','Dan Black','@dan05','tiktok',     'replied',    '00000000-0000-0000-0000-000000000011', now() - interval '1 day',   now() - interval '4 days',  NULL,  '张瑞','feishu', 10,  false),
    ('11111111-1111-1111-1111-000000000006','eve06@example.com','Eve Park','@eve06','instagram',   'replied',    '00000000-0000-0000-0000-000000000012', now() - interval '3 days',  now() - interval '6 days',  NULL,  '李欣','feishu', 12,  false),
    ('11111111-1111-1111-1111-000000000007','frank07@example.com','Frank Lee','@frank07','youtube','negotiating', '00000000-0000-0000-0000-000000000010', now() - interval '8 days',  now() - interval '3 days',  500.0, '王雨','feishu', 15,  false),
    ('11111111-1111-1111-1111-000000000008','grace08@example.com','Grace Tan','@grace08','tiktok', 'negotiating', '00000000-0000-0000-0000-000000000010', now() - interval '6 days',  now() - interval '2 days',  650.0, '王雨','feishu', 18,  false),
    ('11111111-1111-1111-1111-000000000009','henry09@example.com','Henry Wu','@henry09','instagram','negotiating',NULL,                                   NULL,                       NULL,                       700.0, '',    'feishu', 20,  false),
    ('11111111-1111-1111-1111-000000000010','iris10@example.com','Iris Sun','@iris10','youtube',    'confirmed',  '00000000-0000-0000-0000-000000000011', now() - interval '20 days', now() - interval '15 days', 900.0, '张瑞','feishu', 22,  false),
    ('11111111-1111-1111-1111-000000000011','jack11@example.com','Jack Ma','@jack11','tiktok',      'confirmed',  '00000000-0000-0000-0000-000000000011', now() - interval '18 days', now() - interval '12 days',1000.0, '张瑞','feishu', 25,  false),
    ('11111111-1111-1111-1111-000000000012','kate12@example.com','Kate Lin','@kate12','instagram',  'confirmed',  '00000000-0000-0000-0000-000000000012', now() - interval '21 days', now() - interval '14 days', 850.0, '李欣','feishu', 28,  false),
    ('11111111-1111-1111-1111-000000000013','leo13@example.com','Leo Han','@leo13','youtube',       'producing',  '00000000-0000-0000-0000-000000000010', now() - interval '11 days', now() - interval '7 days', 1200.0, '王雨','feishu', 32,  false),
    ('11111111-1111-1111-1111-000000000014','mia14@example.com','Mia Zhao','@mia14','tiktok',       'producing',  '00000000-0000-0000-0000-000000000011', now() - interval '13 days', now() - interval '9 days', 1100.0, '张瑞','feishu', 35,  false),
    ('11111111-1111-1111-1111-000000000015','nora15@example.com','Nora Yang','@nora15','instagram', 'producing',  '00000000-0000-0000-0000-000000000010', now() - interval '15 days', now() - interval '11 days',1300.0, '王雨','feishu', 38,  false),
    ('11111111-1111-1111-1111-000000000016','owen16@example.com','Owen Wei','@owen16','youtube',    'reviewing',  '00000000-0000-0000-0000-000000000011', now() - interval '23 days', now() - interval '20 days',1400.0, '张瑞','feishu', 42,  false),
    ('11111111-1111-1111-1111-000000000017','peggy17@example.com','Peggy Hu','@peggy17','tiktok',   'reviewing',  '00000000-0000-0000-0000-000000000010', now() - interval '24 days', now() - interval '19 days',1500.0, '王雨','feishu', 45,  false),
    ('11111111-1111-1111-1111-000000000018','quinn18@example.com','Quinn Lu','@quinn18','instagram','reviewing',  NULL,                                    NULL,                       NULL,                       1600.0, '',   'manual', 48,  false),
    ('11111111-1111-1111-1111-000000000019','ryan19@example.com','Ryan Xu','@ryan19','youtube',     'published',  '00000000-0000-0000-0000-000000000012', now() - interval '32 days', now() - interval '28 days',1800.0, '李欣','feishu', 52,  true ),
    ('11111111-1111-1111-1111-000000000020','sara20@example.com','Sara Jin','@sara20','tiktok',     'published',  '00000000-0000-0000-0000-000000000012', now() - interval '30 days', now() - interval '26 days',1900.0, '李欣','feishu', 55,  true ),
    ('11111111-1111-1111-1111-000000000021','tom21@example.com','Tom Ren','@tom21','instagram',     'paying',     '00000000-0000-0000-0000-000000000010', now() - interval '35 days', now() - interval '31 days',2000.0, '王雨','feishu', 60,  true ),
    ('11111111-1111-1111-1111-000000000022','uma22@example.com','Uma Pan','@uma22','youtube',       'paying',     '00000000-0000-0000-0000-000000000011', now() - interval '38 days', now() - interval '34 days',2100.0, '张瑞','feishu', 63,  true ),
    ('11111111-1111-1111-1111-000000000023','vera23@example.com','Vera Qi','@vera23','tiktok',      'reinvest',   '00000000-0000-0000-0000-000000000010', now() - interval '50 days', now() - interval '60 days',2500.0, '王雨','manual', 70,  true ),
    ('11111111-1111-1111-1111-000000000024','wade24@example.com','Wade Du','@wade24','instagram',   'reinvest',   '00000000-0000-0000-0000-000000000011', now() - interval '55 days', now() - interval '65 days',2600.0, '张瑞','manual', 75,  false),
    ('11111111-1111-1111-1111-000000000025','xena25@example.com','Xena Mu','@xena25','youtube',     'declined',   '00000000-0000-0000-0000-000000000010', now() - interval '40 days', now() - interval '42 days', NULL, '王雨','gmail',  80,  false),
    ('11111111-1111-1111-1111-000000000026','yan26@example.com','Yan Zhou','@yan26','tiktok',       'declined',   '00000000-0000-0000-0000-000000000012', now() - interval '45 days', now() - interval '47 days', NULL, '李欣','gmail',  85,  false),
    -- 3 "needs my reply" — inbound newer than outbound, no reply_resolved
    ('11111111-1111-1111-1111-000000000027','zoe27@example.com','Zoe Hao','@zoe27','instagram',     'negotiating','00000000-0000-0000-0000-000000000011', now() - interval '1 hour',  now() - interval '3 days',  450.0, '张瑞','gmail',   2,  false),
    ('11111111-1111-1111-1111-000000000028','abby28@example.com','Abby Su','@abby28','youtube',     'replied',    '00000000-0000-0000-0000-000000000010', now() - interval '4 hours', now() - interval '5 days',  NULL,  '王雨','gmail',   4,  false),
    ('11111111-1111-1111-1111-000000000029','beth29@example.com','Beth Wei','@beth29','tiktok',     'producing',  '00000000-0000-0000-0000-000000000012', now() - interval '2 hours', now() - interval '6 days', 1150.0, '李欣','gmail',  16,  false),
    -- 1 unowned for pool view
    ('11111111-1111-1111-1111-000000000030','cody30@example.com','Cody Yu','@cody30','instagram',   'outreach',    NULL,                                  NULL,                       NULL,                       NULL,  '',   'feishu',  9,  false)
) AS seed(id, email, name, handle, platform, stage, owner, last_inbound, last_outbound, price, operator, source, days_ago, reply_resolved)
ON CONFLICT (tenant_id, normalized_email, feishu_operator_name) DO NOTHING;

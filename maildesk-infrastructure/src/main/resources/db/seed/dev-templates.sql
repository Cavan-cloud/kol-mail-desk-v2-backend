-- Dev seed: 5 email templates (Chinese-facing UI text, v3.3 §模板).
-- created_by = leader. Variables in {{...}} style — runtime substitution lives
-- in TemplateService (P5).
--
-- Stable UUIDs: 33333333-3333-3333-3333-00000000000N (N=1..5)
--
-- DEV-ONLY.

INSERT INTO email_templates (id, tenant_id, name, scenario, subject, body, used_count, last_used_at, created_by, updated_by, version)
VALUES
    ('33333333-3333-3333-3333-000000000001'::uuid,
     '00000000-0000-0000-0000-000000000001'::uuid,
     '初次询价（英文）', 'outreach',
     'Collaboration with Lovart — {{kol_name}}',
     E'Hi {{kol_name}},\n\nWe loved your recent content. Lovart would like to explore a paid collaboration. Could you share your rate card?\n\nBest,\n{{operator_name}}',
     12, now() - interval '10 days',
     '00000000-0000-0000-0000-000000000010'::uuid,
     '00000000-0000-0000-0000-000000000010'::uuid, 0),

    ('33333333-3333-3333-3333-000000000002'::uuid,
     '00000000-0000-0000-0000-000000000001'::uuid,
     '议价跟进', 'negotiating',
     'Re: Collaboration — pricing update',
     E'Hi {{kol_name}},\n\nThanks for your reply. Our budget for this campaign sits around ${{agreed_price}}. Would that work on your end?\n\nBest,\n{{operator_name}}',
     8, now() - interval '6 days',
     '00000000-0000-0000-0000-000000000010'::uuid,
     '00000000-0000-0000-0000-000000000010'::uuid, 0),

    ('33333333-3333-3333-3333-000000000003'::uuid,
     '00000000-0000-0000-0000-000000000001'::uuid,
     '制作中提醒', 'producing',
     'Re: Script + draft check-in',
     E'Hi {{kol_name}},\n\nJust a friendly check-in on the script / first draft. Let us know if you need anything from our side.\n\nThanks,\n{{operator_name}}',
     5, now() - interval '4 days',
     '00000000-0000-0000-0000-000000000010'::uuid,
     '00000000-0000-0000-0000-000000000010'::uuid, 0),

    ('33333333-3333-3333-3333-000000000004'::uuid,
     '00000000-0000-0000-0000-000000000001'::uuid,
     '发布后致谢', 'published',
     'Thank you — {{kol_name}} x Lovart',
     E'Hi {{kol_name}},\n\nThanks for the great video! It looks fantastic. We will process the payment shortly.\n\nWarmly,\n{{operator_name}}',
     3, now() - interval '14 days',
     '00000000-0000-0000-0000-000000000010'::uuid,
     '00000000-0000-0000-0000-000000000010'::uuid, 0),

    ('33333333-3333-3333-3333-000000000005'::uuid,
     '00000000-0000-0000-0000-000000000001'::uuid,
     '复投邀请', 'reinvest',
     'Round 2 — would you partner again?',
     E'Hi {{kol_name}},\n\nGreat working with you last time. We have a new campaign launching next month — interested in a follow-up collaboration?\n\nBest,\n{{operator_name}}',
     2, now() - interval '30 days',
     '00000000-0000-0000-0000-000000000010'::uuid,
     '00000000-0000-0000-0000-000000000010'::uuid, 0)
ON CONFLICT (id) DO NOTHING;

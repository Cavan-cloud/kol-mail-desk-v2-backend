-- Dev seed: 100 emails.
-- Distribution:
--   inbound 60 / outbound 40
--   is_read=false on the first 20 (drives unread badges)
--   first 10 carry ai_summary + ai_priority + ai_stage_signal
--   seq 99 & 100 carry ai_error (drives the P4 fallback UI)
-- Each row binds to one of the 30 seeded KOLs round-robin and inherits its owner
-- (NULL owners fall back to the leader so the email gets a non-null user_id).
-- Stable UUIDs: 22222222-2222-2222-2222-<12-digit zero-padded seq>.
--
-- DEV-ONLY.

WITH numbered_kols AS (
    SELECT k.id, k.email, k.name,
           COALESCE(k.owner_user_id, '00000000-0000-0000-0000-000000000010'::uuid) AS owner,
           ROW_NUMBER() OVER (ORDER BY k.id) AS rn
    FROM kols k
    WHERE k.tenant_id = '00000000-0000-0000-0000-000000000001'::uuid
)
INSERT INTO emails (
    id, tenant_id, gmail_message_id, gmail_thread_id,
    kol_id, user_id, direction,
    from_email, to_emails, cc_emails,
    subject, body_text, sent_at,
    ai_summary, ai_priority, ai_stage_signal, ai_extracted_fields, ai_error, ai_processed_at,
    is_read, read_at, read_by,
    created_by, updated_by, version
)
SELECT
    ('22222222-2222-2222-2222-' || lpad(gs.seq::text, 12, '0'))::uuid,
    '00000000-0000-0000-0000-000000000001'::uuid,
    'seed-msg-' || gs.seq,
    'seed-thread-' || ((gs.seq - 1) / 4 + 1),
    nk.id,
    nk.owner,
    CASE WHEN gs.seq <= 60 THEN 'inbound'::email_direction ELSE 'outbound'::email_direction END,
    CASE WHEN gs.seq <= 60 THEN nk.email ELSE 'maildesk-dev@lovart.dev' END,
    CASE WHEN gs.seq <= 60 THEN ARRAY['maildesk-dev@lovart.dev'] ELSE ARRAY[nk.email] END,
    CASE WHEN gs.seq % 7 = 0 THEN ARRAY['leader@lovart.dev'] ELSE NULL END,
    CASE
        WHEN gs.seq <= 60 THEN 'Re: Collaboration follow-up ' || gs.seq
        ELSE 'Lovart x ' || nk.name || ' — pitch ' || gs.seq
    END,
    CASE
        WHEN gs.seq <= 60 THEN 'Thanks for reaching out. ' || nk.name || ' here — happy to chat.'
        ELSE 'Hi ' || nk.name || ', following up on our last conversation.'
    END,
    now() - ((gs.seq % 30) || ' days')::interval - (gs.seq || ' minutes')::interval,
    CASE WHEN gs.seq <= 10 THEN '达人确认报价，可推进合作（AI 摘要 ' || gs.seq || '）' ELSE NULL END,
    CASE WHEN gs.seq <= 10 THEN (ARRAY['high','medium','low'])[((gs.seq - 1) % 3) + 1] ELSE NULL END,
    CASE WHEN gs.seq <= 10 THEN 'negotiating'::kol_stage ELSE NULL END,
    CASE WHEN gs.seq <= 10
         THEN jsonb_build_object('requested_price', 500 + gs.seq * 50)
         ELSE '{}'::jsonb END,
    CASE WHEN gs.seq IN (99, 100) THEN 'AI 分类失败：上游 5xx，已入库等待重新分类' ELSE NULL END,
    CASE WHEN gs.seq <= 10 THEN now() - (gs.seq || ' minutes')::interval ELSE NULL END,
    CASE WHEN gs.seq <= 20 THEN false ELSE true END,
    CASE WHEN gs.seq > 20 THEN now() - (gs.seq || ' hours')::interval ELSE NULL END,
    CASE WHEN gs.seq > 20 THEN nk.owner ELSE NULL END,
    '00000000-0000-0000-0000-000000000010'::uuid,
    '00000000-0000-0000-0000-000000000010'::uuid,
    0
FROM generate_series(1, 100) AS gs(seq)
JOIN numbered_kols nk ON nk.rn = ((gs.seq - 1) % 30) + 1
ON CONFLICT (tenant_id, gmail_message_id, user_id) DO NOTHING;

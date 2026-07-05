-- Dev acceptance fixture: bind sample KOLs + rich HTML emails to the currently
-- logged-in Google OAuth user (aa584821373@gmail.com).
--
-- Run after dev-kols / dev-emails seeds:
--   docker exec -i maildesk-postgres psql -U maildesk -d maildesk \
--     < maildesk-infrastructure/src/main/resources/db/seed/dev-acceptance-fixture.sql
--
-- DEV-ONLY.

-- 1) Assign 3 KOLs to the acceptance tester so「我的」view is non-empty.
UPDATE kols
SET owner_user_id = 'e5be7610-51eb-791a-f9fa-ca749ab45ad6'::uuid,
    updated_at = now()
WHERE id IN (
    '11111111-1111-1111-1111-000000000001'::uuid, -- Alice Wong
    '11111111-1111-1111-1111-000000000004'::uuid, -- Carol Chen
    '11111111-1111-1111-1111-000000000007'::uuid  -- Frank Lee
);

-- 2) Re-home existing seed emails for those KOLs to the acceptance user.
UPDATE emails
SET user_id = 'e5be7610-51eb-791a-f9fa-ca749ab45ad6'::uuid,
    updated_at = now()
WHERE kol_id IN (
    '11111111-1111-1111-1111-000000000001'::uuid,
    '11111111-1111-1111-1111-000000000004'::uuid,
    '11111111-1111-1111-1111-000000000007'::uuid
);

-- 3) Insert one rich HTML inbound email for Alice Wong (links + image + quote chain).
INSERT INTO emails (
    id, tenant_id, gmail_message_id, gmail_thread_id,
    kol_id, user_id, direction,
    from_email, to_emails, cc_emails,
    subject, body_text, body_html, sent_at,
    ai_summary, ai_priority, ai_stage_signal,
    is_read, created_by, updated_by, version
)
VALUES (
    '22222222-2222-2222-2222-000000000101'::uuid,
    '00000000-0000-0000-0000-000000000001'::uuid,
    'acceptance-rich-html-001',
    'acceptance-thread-alice-001',
    '11111111-1111-1111-1111-000000000001'::uuid,
    'e5be7610-51eb-791a-f9fa-ca749ab45ad6'::uuid,
    'inbound'::email_direction,
    'alice01@example.com',
    ARRAY['aa584821373@gmail.com'],
    NULL,
    'Re: Lovart x Alice Wong — summer campaign',
    E'Hi Chloe,\n\nThanks for the follow-up! My TikTok is https://www.tiktok.com/@alice01 and the rate is $800 per video.\n\nOn Tue, Jul 1, 2026 at 10:00 AM Lovart <maildesk-dev@lovart.dev> wrote:\n> Hi Alice, we would love to collaborate on our summer campaign.\n> Could you share your rate card?',
    $html$<p>Hi Chloe,</p>
<p>Thanks for the follow-up! Here is my <a href="https://www.tiktok.com/@alice01">TikTok profile</a> and a sample frame:</p>
<p><img src="https://picsum.photos/seed/maildesk-acceptance/480/270" alt="Sample campaign screenshot" width="480"></p>
<ul><li>Rate: $800 per video</li><li>Turnaround: 5 business days</li></ul>
<blockquote class="gmail_quote">
<div>On Tue, Jul 1, 2026 at 10:00 AM Lovart &lt;maildesk-dev@lovart.dev&gt; wrote:</div>
<div>Hi Alice, we would love to collaborate on our summer campaign.<br>Could you share your rate card?</div>
</blockquote>$html$,
    now() - interval '2 hours',
    '达人确认报价 $800，附主页链接与样图，可推进议价',
    'high',
    'negotiating'::kol_stage,
    false,
    'e5be7610-51eb-791a-f9fa-ca749ab45ad6'::uuid,
    'e5be7610-51eb-791a-f9fa-ca749ab45ad6'::uuid,
    0
)
ON CONFLICT (tenant_id, gmail_message_id, user_id) DO UPDATE SET
    body_text = EXCLUDED.body_text,
    body_html = EXCLUDED.body_html,
    ai_summary = EXCLUDED.ai_summary,
    sent_at = EXCLUDED.sent_at,
    updated_at = now();

-- 4) Seed pool KOLs use status=unassigned (pool view filters by status, not owner alone).
UPDATE kols
SET status = 'unassigned'::kol_status,
    updated_at = now()
WHERE owner_user_id IS NULL
  AND status = 'active'::kol_status;

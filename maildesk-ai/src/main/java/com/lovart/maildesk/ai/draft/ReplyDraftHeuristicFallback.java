package com.lovart.maildesk.ai.draft;

import com.lovart.maildesk.common.enums.KolStage;

/**
 * Template fallback when no AI provider is configured or all providers fail.
 * Ported from legacy {@code lib/ai/client.ts#generateReplyDraft}.
 */
public final class ReplyDraftHeuristicFallback {

    private ReplyDraftHeuristicFallback() {}

    public static ReplyDraftResult draft(ReplyDraftRequest request) {
        return draft(request, null);
    }

    public static ReplyDraftResult draft(ReplyDraftRequest request, String aiError) {
        if (request.kolStage() == KolStage.DECLINED) {
            String english =
                    "This KOL is marked as declined. No reply draft is generated — please review manually.";
            String chinese = "该达人已标记为拒绝合作，未生成回复草稿，请人工处理。";
            return new ReplyDraftResult(english, chinese, true, aiError);
        }

        String kolName = request.kolName();
        String senderName = request.senderName();
        String english =
                """
                Hi %s,

                Thanks for your message. Could you please share your availability, preferred deliverable format, and current rate so we can align internally and move forward quickly?

                Best,
                %s"""
                        .formatted(kolName, senderName);
        String chinese =
                """
                Hi %s，

                感谢你的回复。可以请你分享档期、偏好的内容形式和当前报价吗？我们会据此内部确认并尽快推进。

                Best,
                %s"""
                        .formatted(kolName, senderName);
        return new ReplyDraftResult(english, chinese, true, aiError);
    }
}

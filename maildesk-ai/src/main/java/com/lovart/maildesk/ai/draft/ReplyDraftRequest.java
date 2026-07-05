package com.lovart.maildesk.ai.draft;

import com.lovart.maildesk.common.enums.KolStage;

/**
 * Input for {@link com.lovart.maildesk.ai.AiService#generateReplyDraft(ReplyDraftRequest)}.
 */
public record ReplyDraftRequest(
        String kolName,
        String senderName,
        String latestEmail,
        String history,
        String templateHint,
        KolStage kolStage,
        String kolPlatform,
        Double kolAgreedPrice) {

    public ReplyDraftRequest {
        if (kolName == null || kolName.isBlank()) {
            throw new IllegalArgumentException("kolName is required");
        }
        if (senderName == null || senderName.isBlank()) {
            throw new IllegalArgumentException("senderName is required");
        }
        if (latestEmail == null || latestEmail.isBlank()) {
            throw new IllegalArgumentException("latestEmail is required");
        }
    }
}

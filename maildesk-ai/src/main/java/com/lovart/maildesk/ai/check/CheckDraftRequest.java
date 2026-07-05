package com.lovart.maildesk.ai.check;

import com.lovart.maildesk.common.enums.KolStage;

/**
 * Input for {@link com.lovart.maildesk.ai.AiService#checkDraft(CheckDraftRequest)}.
 */
public record CheckDraftRequest(String draft, KolStage kolStage, String kolPlatform, String kolName) {

    public CheckDraftRequest {
        if (draft == null || draft.isBlank()) {
            throw new IllegalArgumentException("draft is required");
        }
    }
}

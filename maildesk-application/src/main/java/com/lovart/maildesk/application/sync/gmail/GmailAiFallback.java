package com.lovart.maildesk.application.sync.gmail;

import com.lovart.maildesk.ai.classify.EmailClassificationResult;
import com.lovart.maildesk.ai.fallback.AiUserMessages;
import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.common.enums.KolStage;

/**
 * Maps {@link EmailClassificationResult} to Gmail sync persist fields and provides direction-only
 * heuristic when AI is unavailable.
 */
public final class GmailAiFallback {

    private GmailAiFallback() {}

    public static GmailAiFields fromClassification(EmailClassificationResult result) {
        return new GmailAiFields(
                result.stageSignal(),
                result.priority(),
                result.summary(),
                null,
                result.suggestedAction(),
                result.aiError());
    }

    /** Placeholder when the message was already persisted — sync skips the AI call (P4-T08). */
    public static GmailAiFields skippedExisting() {
        return new GmailAiFields(null, null, null, null, null, null);
    }

    /** @deprecated Prefer {@link GmailEmailClassificationService} which calls {@code AiService}. */
    @Deprecated
    public static GmailAiFields classify(EmailDirection direction) {
        return classify(direction, AiUserMessages.CLASSIFY_AI_ERROR);
    }

    public static GmailAiFields classify(EmailDirection direction, String aiError) {
        KolStage stage = direction == EmailDirection.OUTBOUND ? KolStage.OUTREACH : KolStage.REPLIED;
        return new GmailAiFields(
                stage,
                "medium",
                aiError == null ? AiUserMessages.CLASSIFY_HEURISTIC_SUMMARY : AiUserMessages.CLASSIFY_FAILURE_SUMMARY,
                null,
                aiError == null ? AiUserMessages.CLASSIFY_HEURISTIC_ACTION : AiUserMessages.CLASSIFY_FAILURE_ACTION,
                aiError);
    }

    public record GmailAiFields(
            KolStage stageSignal,
            String priority,
            String summary,
            String bodyZh,
            String suggestedAction,
            String aiError) {}
}

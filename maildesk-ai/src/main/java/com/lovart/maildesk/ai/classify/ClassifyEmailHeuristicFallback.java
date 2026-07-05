package com.lovart.maildesk.ai.classify;

import com.fasterxml.jackson.databind.node.NullNode;
import com.lovart.maildesk.ai.fallback.AiUserMessages;
import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.common.enums.KolStage;

import java.util.regex.Pattern;

/**
 * Regex heuristic fallback when no AI provider is configured or all providers fail.
 * Ported from legacy {@code lib/ai/client.ts#classifyEmail} (without {@code body_zh}).
 */
public final class ClassifyEmailHeuristicFallback {

    private static final Pattern PAYING = Pattern.compile("invoice|payment|paypal|wise", Pattern.CASE_INSENSITIVE);
    private static final Pattern NEGOTIATING = Pattern.compile("rate|price|budget|quote|USD|\\$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HIGH_PRIORITY =
            Pattern.compile("rate|price|invoice|payment|script|draft", Pattern.CASE_INSENSITIVE);

    private ClassifyEmailHeuristicFallback() {
    }

    public static EmailClassificationResult classify(ClassifyEmailRequest request) {
        return classify(request, null);
    }

    public static EmailClassificationResult classify(ClassifyEmailRequest request, String aiError) {
        String body = request.body();
        KolStage stage;
        if (PAYING.matcher(body).find()) {
            stage = KolStage.PAYING;
        } else if (NEGOTIATING.matcher(body).find()) {
            stage = KolStage.NEGOTIATING;
        } else if (request.direction() == EmailDirection.OUTBOUND) {
            stage = KolStage.OUTREACH;
        } else {
            stage = KolStage.REPLIED;
        }

        String priority = HIGH_PRIORITY.matcher(body).find() ? "high" : "medium";
        String summary = aiError == null ? AiUserMessages.CLASSIFY_HEURISTIC_SUMMARY : AiUserMessages.CLASSIFY_FAILURE_SUMMARY;
        String suggestedAction = aiError == null ? AiUserMessages.CLASSIFY_HEURISTIC_ACTION : AiUserMessages.CLASSIFY_FAILURE_ACTION;

        return new EmailClassificationResult(
                stage,
                priority,
                summary,
                suggestedAction,
                NullNode.getInstance(),
                true,
                aiError);
    }
}

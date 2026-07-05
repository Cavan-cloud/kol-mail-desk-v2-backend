package com.lovart.maildesk.ai.classify;

import com.fasterxml.jackson.databind.JsonNode;
import com.lovart.maildesk.common.enums.KolStage;

/**
 * Strict JSON classification output (no {@code body_zh}; translation is on-demand in P4-T06).
 */
public record EmailClassificationResult(
        KolStage stageSignal,
        String priority,
        String summary,
        String suggestedAction,
        JsonNode extractedFields,
        boolean fallback,
        String aiError) {
}

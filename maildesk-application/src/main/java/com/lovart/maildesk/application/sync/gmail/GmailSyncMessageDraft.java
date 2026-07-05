package com.lovart.maildesk.application.sync.gmail;

import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.domain.gmail.GmailFullMessage;

public record GmailSyncMessageDraft(
        GmailFullMessage parsed,
        EmailDirection direction,
        String counterpartyEmail,
        KolStage aiStageSignal,
        String aiPriority,
        String aiSummary,
        String bodyZh,
        String aiSuggestedAction,
        String aiError,
        boolean aiClassificationSkipped) {
}

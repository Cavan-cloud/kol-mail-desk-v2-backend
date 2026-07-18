package com.lovart.maildesk.application.ai;

import com.lovart.maildesk.ai.AiService;
import com.lovart.maildesk.ai.check.CheckDraftRequest;
import com.lovart.maildesk.ai.check.CheckDraftResult;
import com.lovart.maildesk.ai.draft.ReplyDraftRequest;
import com.lovart.maildesk.ai.draft.ReplyDraftResult;
import com.lovart.maildesk.ai.translate.TranslateMode;
import com.lovart.maildesk.ai.translate.TranslateTargetLang;
import com.lovart.maildesk.ai.translate.TranslateTextRequest;
import com.lovart.maildesk.ai.translate.TranslateTextResult;
import com.lovart.maildesk.application.dto.AiCheckRequest;
import com.lovart.maildesk.application.dto.AiCheckResult;
import com.lovart.maildesk.application.dto.AiDraftRequest;
import com.lovart.maildesk.application.dto.AiDraftResult;
import com.lovart.maildesk.application.dto.AiTranslateRequest;
import com.lovart.maildesk.application.dto.AiTranslateResult;
import com.lovart.maildesk.common.enums.KolStage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiApplicationService {

    private static final String DEFAULT_SENDER = "Chloe";

    private final AiService ai;

    public AiApplicationService(AiService ai) {
        this.ai = ai;
    }

    public AiDraftResult generateDraft(AiDraftRequest request) {
        ReplyDraftResult result = ai.generateReplyDraft(new ReplyDraftRequest(
                request.kolName(),
                blankToDefault(request.senderName(), DEFAULT_SENDER),
                request.latestEmail(),
                request.history() == null ? "" : request.history(),
                request.templateHint(),
                parseStage(request.kolStage()),
                request.kolPlatform(),
                request.kolAgreedPrice()));
        return new AiDraftResult(result.english(), result.chinese(), result.fallback());
    }

    public AiCheckResult checkDraft(AiCheckRequest request) {
        CheckDraftResult result = ai.checkDraft(new CheckDraftRequest(
                request.draft(),
                null,
                null,
                request.context()));
        List<AiCheckResult.AiCheckIssue> issues = new ArrayList<>();
        if (result.issues() != null) {
            for (String issue : result.issues()) {
                if (issue != null && !issue.isBlank()) {
                    issues.add(new AiCheckResult.AiCheckIssue("medium", issue));
                }
            }
        }
        return new AiCheckResult(issues, result.fallback());
    }

    public AiTranslateResult translate(AiTranslateRequest request) {
        TranslateTargetLang target = TranslateTargetLang.fromApiValue(request.targetLang());
        TranslateMode mode = target.isSendDraftTarget() ? TranslateMode.SEND_DRAFT : TranslateMode.EMAIL_BODY;
        TranslateTextResult result = ai.translateText(new TranslateTextRequest(request.text(), target, mode));
        return new AiTranslateResult(
                result.translated(),
                result.targetLang().apiValue(),
                result.fallback());
    }

    private static KolStage parseStage(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return KolStage.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}

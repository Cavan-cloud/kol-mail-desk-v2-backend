package com.lovart.maildesk.application.sync.gmail;

import com.lovart.maildesk.ai.AiService;
import com.lovart.maildesk.ai.classify.ClassifyEmailRequest;
import com.lovart.maildesk.ai.classify.EmailClassificationResult;
import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.domain.gmail.GmailFullMessage;
import com.lovart.maildesk.integration.gmail.GmailProperties;
import org.springframework.stereotype.Service;

/**
 * Classifies Gmail messages during sync. Never throws — failures degrade to heuristic fields so
 * emails still persist (P4-T07).
 */
@Service
public class GmailEmailClassificationService {

    private final AiService aiService;
    private final GmailProperties gmailProperties;

    public GmailEmailClassificationService(AiService aiService, GmailProperties gmailProperties) {
        this.aiService = aiService;
        this.gmailProperties = gmailProperties;
    }

    public GmailAiFallback.GmailAiFields classify(GmailFullMessage message, EmailDirection direction) {
        return classify(message, direction, gmailProperties.aiClassificationEnabled());
    }

    /**
     * @param useAi when false, uses inbound/outbound direction only (no LLM). Manual reclassify passes
     *              {@code true} so operators can still trigger AI on demand.
     */
    public GmailAiFallback.GmailAiFields classify(
            GmailFullMessage message, EmailDirection direction, boolean useAi) {
        if (!useAi) {
            return GmailAiFallback.fromDirectionOnly(direction);
        }
        String body = resolveBody(message);
        ClassifyEmailRequest request = new ClassifyEmailRequest(
                direction,
                message.subject() == null ? "" : message.subject(),
                body,
                "");
        EmailClassificationResult result = aiService.classifyEmail(request);
        return GmailAiFallback.fromClassification(result);
    }

    private static String resolveBody(GmailFullMessage message) {
        if (message.bodyText() != null && !message.bodyText().isBlank()) {
            return message.bodyText();
        }
        if (message.bodyHtml() != null && !message.bodyHtml().isBlank()) {
            return message.bodyHtml();
        }
        return "";
    }
}

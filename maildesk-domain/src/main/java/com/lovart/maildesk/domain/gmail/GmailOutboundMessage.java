package com.lovart.maildesk.domain.gmail;

import java.util.List;

/**
 * Outbound MIME payload for Gmail {@code users.messages.send}.
 */
public record GmailOutboundMessage(
        String to,
        List<String> ccEmails,
        String subject,
        String bodyText,
        String bodyHtml) {

    public GmailOutboundMessage {
        ccEmails = ccEmails == null ? List.of() : List.copyOf(ccEmails);
    }
}

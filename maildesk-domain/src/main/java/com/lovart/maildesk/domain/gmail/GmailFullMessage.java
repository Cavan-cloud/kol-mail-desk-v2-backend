package com.lovart.maildesk.domain.gmail;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Parsed Gmail message (format=full) ready for application-layer persist.
 */
public record GmailFullMessage(
        String gmailMessageId,
        String gmailThreadId,
        String historyId,
        String fromEmail,
        List<String> toEmails,
        List<String> ccEmails,
        String subject,
        String bodyText,
        String bodyHtml,
        List<String> attachmentNames,
        boolean hasAttachments,
        OffsetDateTime sentAt,
        List<String> labelIds) {
}

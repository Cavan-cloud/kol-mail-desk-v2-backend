package com.lovart.maildesk.domain.gmail;

/**
 * Gmail API response for a successfully sent message.
 */
public record GmailSentMessage(String messageId, String threadId) {}

package com.lovart.maildesk.domain.gmail;

/**
 * Gmail API port (sync read + outbound send).
 */
public interface GmailClient {

    GmailMessageListPage listRecentMessages(String accessToken, int maxResults, int historyDays, String pageToken);

    GmailHistoryPage listIncrementalMessageIds(
            String accessToken, String startHistoryId, int maxResults);

    GmailFullMessage getMessage(String accessToken, String messageId);

    GmailSentMessage sendMessage(String accessToken, GmailOutboundMessage message);
}

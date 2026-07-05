package com.lovart.maildesk.application.sync.gmail;

public record GmailSyncResult(
        String status,
        String message,
        int processed,
        String mode,
        String nextPageToken,
        String historyId,
        int insertedEmails) {

    public static GmailSyncResult notConfigured(String message) {
        return new GmailSyncResult("not_configured", message, 0, null, null, null, 0);
    }
}

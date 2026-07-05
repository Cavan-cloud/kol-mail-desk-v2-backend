package com.lovart.maildesk.application.sync.gmail;

public record GmailSyncOptions(
        String mode,
        int maxResults,
        int historyDays,
        String pageToken,
        boolean forceRecent) {

    public static GmailSyncOptions incremental() {
        return new GmailSyncOptions("incremental", 50, 60, null, false);
    }

    public static GmailSyncOptions historyPage(String pageToken) {
        return new GmailSyncOptions("history", 30, 365, pageToken, true);
    }

    public boolean historyMode() {
        return "history".equals(mode);
    }
}

package com.lovart.maildesk.application.dto;

import java.time.OffsetDateTime;

public record GmailSyncStatusDto(
        boolean running,
        String mode,
        int processed,
        String nextPageToken,
        OffsetDateTime lastSyncedAt,
        String lastError) {

    public static GmailSyncStatusDto idle() {
        return new GmailSyncStatusDto(false, null, 0, null, null, null);
    }

    public static GmailSyncStatusDto startRunning(String mode) {
        return new GmailSyncStatusDto(true, mode, 0, null, null, null);
    }

    public GmailSyncStatusDto withRunning(boolean value) {
        return new GmailSyncStatusDto(value, mode, processed, nextPageToken, lastSyncedAt, lastError);
    }
}

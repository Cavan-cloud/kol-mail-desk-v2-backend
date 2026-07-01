package com.lovart.maildesk.application.dto;

import java.time.OffsetDateTime;

/**
 * API-facing Feishu sync progress ({@code FeishuSyncStatus} in OpenAPI).
 */
public record FeishuSyncStatusDto(
        boolean running,
        int upserted,
        OffsetDateTime lastSyncedAt,
        String lastError) {

    public static FeishuSyncStatusDto idle() {
        return new FeishuSyncStatusDto(false, 0, null, null);
    }

    public static FeishuSyncStatusDto startRunning() {
        return new FeishuSyncStatusDto(true, 0, null, null);
    }

    public FeishuSyncStatusDto withRunning(boolean value) {
        return new FeishuSyncStatusDto(value, upserted, lastSyncedAt, lastError);
    }
}

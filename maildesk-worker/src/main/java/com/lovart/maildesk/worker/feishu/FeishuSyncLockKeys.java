package com.lovart.maildesk.worker.feishu;

/**
 * Redis lock namespaces for Feishu sync entry points (delta job, backfill CLI, future manual worker triggers).
 */
public final class FeishuSyncLockKeys {

    /** Shared lock — only one Feishu sync run (delta / backfill / future) at a time per deployment. */
    public static final String SYNC = "maildesk:lock:feishu-sync";

    private FeishuSyncLockKeys() {
    }
}

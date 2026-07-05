package com.lovart.maildesk.worker.gmail;

/**
 * Redis lock keys for Gmail sync jobs.
 */
public final class GmailSyncLockKeys {

    public static final String INCREMENTAL = "maildesk:lock:gmail-incremental-sync";

    private GmailSyncLockKeys() {
    }
}

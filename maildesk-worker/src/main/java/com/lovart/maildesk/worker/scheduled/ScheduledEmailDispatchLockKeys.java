package com.lovart.maildesk.worker.scheduled;

/**
 * Redis lock key for scheduled email dispatch (optional guard; row claim is authoritative).
 */
public final class ScheduledEmailDispatchLockKeys {

    public static final String DISPATCH = "maildesk:lock:scheduled-email-dispatch";

    private ScheduledEmailDispatchLockKeys() {}
}

package com.lovart.maildesk.common.exception;

/**
 * Gmail API or OAuth token failures surfaced to application layer.
 */
public class GmailIntegrationException extends RuntimeException {

    private final boolean credentialExpired;

    public GmailIntegrationException(String message) {
        this(message, false);
    }

    public GmailIntegrationException(String message, boolean credentialExpired) {
        super(message);
        this.credentialExpired = credentialExpired;
    }

    public GmailIntegrationException(String message, Throwable cause) {
        super(message, cause);
        this.credentialExpired = false;
    }

    public boolean credentialExpired() {
        return credentialExpired;
    }
}

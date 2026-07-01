package com.lovart.maildesk.common.exception;

/**
 * Feishu Open API call failed after retries or returned a non-zero {@code code}.
 */
public class FeishuIntegrationException extends RuntimeException {

    private final Integer feishuCode;

    public FeishuIntegrationException(String message) {
        super(message);
        this.feishuCode = null;
    }

    public FeishuIntegrationException(String message, Throwable cause) {
        super(message, cause);
        this.feishuCode = null;
    }

    public FeishuIntegrationException(int feishuCode, String message) {
        super(message);
        this.feishuCode = feishuCode;
    }

    public Integer feishuCode() {
        return feishuCode;
    }
}

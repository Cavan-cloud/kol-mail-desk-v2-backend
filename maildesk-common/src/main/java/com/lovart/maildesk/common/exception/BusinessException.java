package com.lovart.maildesk.common.exception;

/**
 * Expected business rule violation surfaced to the client as {@code ApiError}.
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}

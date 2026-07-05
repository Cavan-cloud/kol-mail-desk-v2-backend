package com.lovart.maildesk.ai.fallback;

/**
 * Classified reason for an AI provider call failure (used for logging and fallback routing).
 */
public enum AiFailureReason {
    NO_PROVIDER,
    UNAUTHORIZED,
    RATE_LIMIT,
    INSUFFICIENT_BALANCE,
    TIMEOUT,
    INVALID_RESPONSE,
    UNKNOWN
}

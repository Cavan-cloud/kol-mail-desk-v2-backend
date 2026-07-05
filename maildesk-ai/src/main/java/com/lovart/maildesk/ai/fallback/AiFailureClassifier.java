package com.lovart.maildesk.ai.fallback;

import java.util.Locale;
import java.util.function.Function;

/**
 * Maps provider exceptions to a coarse failure reason for logging and diagnostics.
 */
public final class AiFailureClassifier {

    private AiFailureClassifier() {}

    public static AiFailureReason classify(Throwable throwable) {
        if (throwable == null) {
            return AiFailureReason.UNKNOWN;
        }
        String message = messageChain(throwable);
        if (message.contains("401") || message.contains("unauthorized") || message.contains("invalid api key")) {
            return AiFailureReason.UNAUTHORIZED;
        }
        if (message.contains("429") || message.contains("rate limit") || message.contains("too many requests")) {
            return AiFailureReason.RATE_LIMIT;
        }
        if (message.contains("timeout")
                || message.contains("timed out")
                || message.contains("read timed out")
                || message.contains("connect timed out")) {
            return AiFailureReason.TIMEOUT;
        }
        if (message.contains("402")
                || message.contains("insufficient")
                || message.contains("balance")
                || message.contains("quota")
                || message.contains("余额")
                || message.contains("exceeded")) {
            return AiFailureReason.INSUFFICIENT_BALANCE;
        }
        if (message.contains("empty")
                || message.contains("not valid json")
                || message.contains("is required")
                || message.contains("invalid priority")) {
            return AiFailureReason.INVALID_RESPONSE;
        }
        return AiFailureReason.UNKNOWN;
    }

    private static String messageChain(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append(' ');
                }
                builder.append(current.getMessage());
            }
            current = current.getCause();
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }
}

package com.lovart.maildesk.ai.usage;

import java.util.Optional;

public record AiInvocationAttempt<T>(Optional<T> result, AiCallMetrics metrics) {

    public static <T> AiInvocationAttempt<T> success(T value, AiCallMetrics metrics) {
        return new AiInvocationAttempt<>(Optional.of(value), metrics);
    }

    public static <T> AiInvocationAttempt<T> failure(AiCallMetrics metrics) {
        return new AiInvocationAttempt<>(Optional.empty(), metrics);
    }
}

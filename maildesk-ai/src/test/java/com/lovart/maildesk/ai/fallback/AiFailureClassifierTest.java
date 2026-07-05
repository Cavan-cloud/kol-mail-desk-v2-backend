package com.lovart.maildesk.ai.fallback;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiFailureClassifierTest {

    @Test
    void classifiesUnauthorized() {
        assertThat(AiFailureClassifier.classify(new RuntimeException("401 unauthorized")))
                .isEqualTo(AiFailureReason.UNAUTHORIZED);
    }

    @Test
    void classifiesRateLimit() {
        assertThat(AiFailureClassifier.classify(new RuntimeException("429 rate limit exceeded")))
                .isEqualTo(AiFailureReason.RATE_LIMIT);
    }

    @Test
    void classifiesTimeout() {
        assertThat(AiFailureClassifier.classify(new RuntimeException("Read timed out")))
                .isEqualTo(AiFailureReason.TIMEOUT);
    }

    @Test
    void classifiesInsufficientBalance() {
        assertThat(AiFailureClassifier.classify(new RuntimeException("Insufficient balance for this request")))
                .isEqualTo(AiFailureReason.INSUFFICIENT_BALANCE);
    }

    @Test
    void classifiesInvalidResponse() {
        assertThat(AiFailureClassifier.classify(new IllegalArgumentException("priority is required")))
                .isEqualTo(AiFailureReason.INVALID_RESPONSE);
    }

    @Test
    void walksCauseChain() {
        RuntimeException root = new RuntimeException("401 unauthorized");
        RuntimeException wrapped = new RuntimeException("upstream failed", root);
        assertThat(AiFailureClassifier.classify(wrapped)).isEqualTo(AiFailureReason.UNAUTHORIZED);
    }
}

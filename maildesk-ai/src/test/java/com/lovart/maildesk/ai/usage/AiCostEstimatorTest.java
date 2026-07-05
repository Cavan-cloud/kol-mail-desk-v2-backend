package com.lovart.maildesk.ai.usage;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AiCostEstimatorTest {

    @Test
    void estimatesMoonshot8kCost() {
        BigDecimal cost = AiCostEstimator.estimate("moonshot", "moonshot-v1-8k", 1000, 500);

        assertThat(cost).isEqualByComparingTo("0.01800000");
    }

    @Test
    void estimatesDeepSeekFlashCost() {
        BigDecimal cost = AiCostEstimator.estimate("deepseek", "deepseek-v4-flash", 2000, 1000);

        assertThat(cost).isEqualByComparingTo("0.00600000");
    }

    @Test
    void returnsNullWhenTokensMissing() {
        assertThat(AiCostEstimator.estimate("moonshot", "moonshot-v1-8k", null, null)).isNull();
    }
}

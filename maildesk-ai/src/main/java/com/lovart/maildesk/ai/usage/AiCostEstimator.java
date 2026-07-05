package com.lovart.maildesk.ai.usage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

/**
 * Approximate CNY cost from published list prices (2026-07, see {@code 02-backend-design.md} §2.8).
 * Used for {@code ai_usage_log.estimated_cost_cny} — re-calibrate with real usage data later.
 */
public final class AiCostEstimator {

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    /** Unified input+output price per million tokens (CNY). */
    private static final Map<String, BigDecimal> PRICE_PER_MILLION = Map.ofEntries(
            Map.entry(key("moonshot", "moonshot-v1-8k"), new BigDecimal("12")),
            Map.entry(key("moonshot", "moonshot-v1-32k"), new BigDecimal("24")),
            Map.entry(key("moonshot", "moonshot-v1-128k"), new BigDecimal("60")),
            Map.entry(key("deepseek", "deepseek-v4-flash"), new BigDecimal("2")),
            Map.entry(key("deepseek", "deepseek-v4-pro"), new BigDecimal("8")));

    private AiCostEstimator() {}

    public static BigDecimal estimate(
            String provider, String model, Integer promptTokens, Integer completionTokens) {
        if (promptTokens == null && completionTokens == null) {
            return null;
        }
        int prompt = promptTokens == null ? 0 : Math.max(promptTokens, 0);
        int completion = completionTokens == null ? 0 : Math.max(completionTokens, 0);
        if (prompt == 0 && completion == 0) {
            return BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
        }
        BigDecimal price = PRICE_PER_MILLION.get(key(provider, model));
        if (price == null) {
            return null;
        }
        BigDecimal totalTokens = BigDecimal.valueOf(prompt + completion);
        return totalTokens
                .multiply(price)
                .divide(ONE_MILLION, 8, RoundingMode.HALF_UP);
    }

    private static String key(String provider, String model) {
        return (provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT))
                + "|"
                + (model == null ? "" : model.trim().toLowerCase(Locale.ROOT));
    }
}

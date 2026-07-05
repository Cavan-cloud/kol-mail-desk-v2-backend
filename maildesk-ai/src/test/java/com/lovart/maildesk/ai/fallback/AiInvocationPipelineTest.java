package com.lovart.maildesk.ai.fallback;

import com.lovart.maildesk.ai.AiCapability;
import com.lovart.maildesk.ai.AiModelRouter;
import com.lovart.maildesk.ai.config.AiProviderProperties;
import com.lovart.maildesk.ai.usage.AiCallMetrics;
import com.lovart.maildesk.ai.usage.AiInvocationAttempt;
import com.lovart.maildesk.domain.usage.AiUsageEntry;
import com.lovart.maildesk.domain.usage.AiUsageLogPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AiInvocationPipelineTest {

    @Mock
    private ChatModel moonshotModel;

    @Mock
    private ChatModel deepseekModel;

    @Test
    void runsPrimaryThenFallbackThenHeuristic() {
        AiProviderProperties properties = propertiesWithFallback();
        AiModelRouter router = new AiModelRouter(
                properties, Map.of("moonshot", moonshotModel, "deepseek", deepseekModel));
        AiInvocationPipeline pipeline = new AiInvocationPipeline(router);
        AtomicInteger attempts = new AtomicInteger();

        String result = pipeline.run(
                AiCapability.CLASSIFY,
                "classifyEmail",
                target -> {
                    attempts.incrementAndGet();
                    return AiInvocationAttempt.failure(
                            AiCallMetrics.of(target, false, 1, null, null));
                },
                new AiInvocationFallbacks<>(() -> "no-provider", () -> "all-failed"));

        assertThat(result).isEqualTo("all-failed");
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void returnsNoProviderFallbackWhenUnconfigured() {
        AiInvocationPipeline pipeline =
                new AiInvocationPipeline(new AiModelRouter(new AiProviderProperties(), Map.of()));

        String result = pipeline.run(
                AiCapability.TRANSLATE,
                "translateText",
                target -> AiInvocationAttempt.success("should-not-run", AiCallMetrics.of(target, true, 1, 1, 1)),
                new AiInvocationFallbacks<>(() -> "no-provider", () -> "all-failed"));

        assertThat(result).isEqualTo("no-provider");
    }

    @Test
    void returnsPrimaryResultWithoutFallback() {
        AiProviderProperties properties = propertiesWithFallback();
        AiModelRouter router = new AiModelRouter(
                properties, Map.of("moonshot", moonshotModel, "deepseek", deepseekModel));
        AiInvocationPipeline pipeline = new AiInvocationPipeline(router);

        String result = pipeline.run(
                AiCapability.CLASSIFY,
                "classifyEmail",
                target -> AiInvocationAttempt.success("ok-" + target.providerId(), AiCallMetrics.of(target, true, 2, 10, 20)),
                new AiInvocationFallbacks<>(() -> "no-provider", () -> "all-failed"));

        assertThat(result).isEqualTo("ok-moonshot");
    }

    @Test
    void recordsUsageForEachProviderAttempt() {
        AiProviderProperties properties = propertiesWithFallback();
        AiModelRouter router = new AiModelRouter(
                properties, Map.of("moonshot", moonshotModel, "deepseek", deepseekModel));
        List<AiUsageEntry> entries = new ArrayList<>();
        AiUsageLogPort usageLog = entries::add;
        AiInvocationPipeline pipeline = new AiInvocationPipeline(router, usageLog);

        pipeline.run(
                AiCapability.CLASSIFY,
                "classifyEmail",
                target -> AiInvocationAttempt.failure(AiCallMetrics.of(target, false, 5, null, null)),
                new AiInvocationFallbacks<>(() -> "no-provider", () -> "all-failed"));

        assertThat(entries).hasSize(3);
        assertThat(entries.get(0).provider()).isEqualTo("moonshot");
        assertThat(entries.get(1).provider()).isEqualTo("deepseek");
        assertThat(entries.get(2).provider()).isEqualTo("heuristic");
    }

    private static AiProviderProperties propertiesWithFallback() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setDefaultProvider("moonshot");
        AiProviderProperties.Provider moonshot = new AiProviderProperties.Provider();
        moonshot.setBaseUrl("https://api.moonshot.cn/v1");
        moonshot.setApiKey("moonshot-key");
        AiProviderProperties.Provider deepseek = new AiProviderProperties.Provider();
        deepseek.setBaseUrl("https://api.deepseek.com");
        deepseek.setApiKey("deepseek-key");
        properties.setProviders(Map.of("moonshot", moonshot, "deepseek", deepseek));
        AiProviderProperties.CapabilityBinding binding = new AiProviderProperties.CapabilityBinding();
        binding.setProvider("moonshot");
        binding.setModel("moonshot-v1-8k");
        binding.setFallbackProvider("deepseek");
        binding.setFallbackModel("deepseek-v4-flash");
        properties.setCapabilities(Map.of("classify", binding));
        return properties;
    }
}

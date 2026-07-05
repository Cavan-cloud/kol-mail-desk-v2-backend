package com.lovart.maildesk.ai;

import com.lovart.maildesk.ai.config.AiProviderProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AiModelRouterTest {

    @Mock
    private ChatModel moonshotModel;

    @Mock
    private ChatModel deepseekModel;

    private AiProviderProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AiProviderProperties();
        properties.setDefaultProvider("moonshot");

        AiProviderProperties.Provider moonshot = new AiProviderProperties.Provider();
        moonshot.setBaseUrl("https://api.moonshot.cn/v1");
        moonshot.setApiKey("moonshot-key");

        AiProviderProperties.Provider deepseek = new AiProviderProperties.Provider();
        deepseek.setBaseUrl("https://api.deepseek.com");
        deepseek.setApiKey("deepseek-key");

        properties.setProviders(Map.of("moonshot", moonshot, "deepseek", deepseek));

        AiProviderProperties.CapabilityBinding classify = new AiProviderProperties.CapabilityBinding();
        classify.setProvider("moonshot");
        classify.setModel("moonshot-v1-8k");
        classify.setFallbackProvider("deepseek");
        classify.setFallbackModel("deepseek-v4-flash");

        AiProviderProperties.CapabilityBinding draft = new AiProviderProperties.CapabilityBinding();
        draft.setModel("moonshot-v1-128k");

        properties.setCapabilities(Map.of(
                "classify", classify,
                "draft", draft));
    }

    @Test
    void resolvePrimaryUsesCapabilityProviderAndModel() {
        AiModelRouter router = new AiModelRouter(
                properties, Map.of("moonshot", moonshotModel, "deepseek", deepseekModel));

        Optional<AiResolvedTarget> target = router.resolvePrimary(AiCapability.CLASSIFY);

        assertThat(target).isPresent();
        assertThat(target.get().providerId()).isEqualTo("moonshot");
        assertThat(target.get().model()).isEqualTo("moonshot-v1-8k");
        assertThat(target.get().fallback()).isFalse();
        assertThat(target.get().chatModel()).isSameAs(moonshotModel);
    }

    @Test
    void resolvePrimaryFallsBackToDefaultProviderWhenCapabilityProviderBlank() {
        AiModelRouter router = new AiModelRouter(properties, Map.of("moonshot", moonshotModel));

        Optional<AiResolvedTarget> target = router.resolvePrimary(AiCapability.DRAFT);

        assertThat(target).isPresent();
        assertThat(target.get().providerId()).isEqualTo("moonshot");
        assertThat(target.get().model()).isEqualTo("moonshot-v1-128k");
    }

    @Test
    void resolveFallbackUsesConfiguredFallbackProvider() {
        AiModelRouter router = new AiModelRouter(
                properties, Map.of("moonshot", moonshotModel, "deepseek", deepseekModel));

        Optional<AiResolvedTarget> target = router.resolveFallback(AiCapability.CLASSIFY);

        assertThat(target).isPresent();
        assertThat(target.get().providerId()).isEqualTo("deepseek");
        assertThat(target.get().model()).isEqualTo("deepseek-v4-flash");
        assertThat(target.get().fallback()).isTrue();
    }

    @Test
    void resolvePrimaryEmptyWhenProviderApiKeyMissing() {
        properties.getProviders().get("moonshot").setApiKey("");
        AiModelRouter router = new AiModelRouter(properties, Map.of("moonshot", moonshotModel));

        assertThat(router.resolvePrimary(AiCapability.CLASSIFY)).isEmpty();
    }

    @Test
    void resolveFallbackEmptyWhenNotConfigured() {
        AiModelRouter router = new AiModelRouter(properties, Map.of("moonshot", moonshotModel));

        assertThat(router.resolveFallback(AiCapability.DRAFT)).isEmpty();
    }
}

package com.lovart.maildesk.ai.prompt;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Loads static system prompts from {@code resources/prompts/*.st}.
 * <p>
 * Files use the {@code .st} extension per project convention but are read as plain UTF-8 text
 * (no StringTemplate placeholders) because prompts embed JSON schema examples with literal braces.
 */
public class AiPromptCatalog {

    private final Map<AiPromptKey, String> systemPrompts;

    public AiPromptCatalog() {
        this(loadAll());
    }

    AiPromptCatalog(Map<AiPromptKey, String> systemPrompts) {
        this.systemPrompts = Map.copyOf(systemPrompts);
    }

    public String systemPrompt(AiPromptKey key) {
        String prompt = systemPrompts.get(key);
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalStateException("Missing AI system prompt: " + key);
        }
        return prompt;
    }

    public boolean contains(AiPromptKey key) {
        return systemPrompts.containsKey(key);
    }

    private static Map<AiPromptKey, String> loadAll() {
        Map<AiPromptKey, String> loaded = new EnumMap<>(AiPromptKey.class);
        for (AiPromptKey key : AiPromptKey.values()) {
            loaded.put(key, readClasspath(key.classpathLocation()));
        }
        return loaded;
    }

    private static String readClasspath(String location) {
        ClassPathResource resource = new ClassPathResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("AI prompt resource not found: " + location);
        }
        try (InputStream in = resource.getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read AI prompt: " + location, ex);
        }
    }
}

package com.lovart.maildesk.ai.classify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.lovart.maildesk.common.enums.KolStage;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and validates LLM JSON output for email classification.
 */
public class EmailClassificationParser {

    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*\\}");
    private static final Set<String> PRIORITIES = Set.of("high", "medium", "low");

    private final ObjectMapper objectMapper;

    public EmailClassificationParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EmailClassificationResult parse(String rawContent) {
        JsonNode root = readJsonObject(rawContent);

        KolStage stageSignal = parseStageSignal(text(root, "stage_signal"));
        String priority = parsePriority(text(root, "priority"));
        String summary = requiredText(root, "summary");
        String suggestedAction = requiredText(root, "suggested_action");
        JsonNode extracted = root.path("extracted");
        if (extracted.isMissingNode() || extracted.isNull()) {
            extracted = NullNode.getInstance();
        }

        return new EmailClassificationResult(
                stageSignal, priority, summary, suggestedAction, extracted, false, null);
    }

    private JsonNode readJsonObject(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalArgumentException("AI returned empty content");
        }
        String trimmed = rawContent.trim();
        try {
            return objectMapper.readTree(trimmed);
        } catch (Exception ignored) {
            Matcher matcher = JSON_OBJECT.matcher(trimmed);
            if (!matcher.find()) {
                throw new IllegalArgumentException("AI output is not a JSON object");
            }
            try {
                return objectMapper.readTree(matcher.group());
            } catch (Exception ex) {
                throw new IllegalArgumentException("AI output is not valid JSON", ex);
            }
        }
    }

    private static KolStage parseStageSignal(String value) {
        if (value == null || value.isBlank()) {
            return KolStage.REPLIED;
        }
        try {
            return KolStage.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return KolStage.REPLIED;
        }
    }

    private static String parsePriority(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("priority is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!PRIORITIES.contains(normalized)) {
            throw new IllegalArgumentException("invalid priority: " + value);
        }
        return normalized;
    }

    private static String requiredText(JsonNode root, String field) {
        String value = text(root, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }
}

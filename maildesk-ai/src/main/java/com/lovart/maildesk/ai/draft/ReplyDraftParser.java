package com.lovart.maildesk.ai.draft;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and validates LLM JSON output for reply draft generation.
 */
public class ReplyDraftParser {

    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*\\}");

    private final ObjectMapper objectMapper;

    public ReplyDraftParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ReplyDraftResult parse(String rawContent) {
        JsonNode root = readJsonObject(rawContent);
        String english = requiredText(root, "english");
        String chinese = requiredText(root, "chinese");
        return new ReplyDraftResult(english, chinese, false, null);
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

    private static String requiredText(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String value = node.asText();
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}

package com.lovart.maildesk.ai.check;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and validates LLM JSON output for pre-send draft review.
 */
public class CheckDraftParser {

    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*\\}");

    private final ObjectMapper objectMapper;

    public CheckDraftParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CheckDraftResult parse(String rawContent) {
        JsonNode root = readJsonObject(rawContent);
        JsonNode issuesNode = root.get("issues");
        if (issuesNode == null || issuesNode.isNull() || !issuesNode.isArray()) {
            throw new IllegalArgumentException("issues array is required");
        }
        List<String> issues = new ArrayList<>();
        for (JsonNode item : issuesNode) {
            if (item != null && !item.isNull()) {
                String text = item.asText().trim();
                if (!text.isEmpty()) {
                    issues.add(text);
                }
            }
        }
        return new CheckDraftResult(List.copyOf(issues), false, null);
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
}

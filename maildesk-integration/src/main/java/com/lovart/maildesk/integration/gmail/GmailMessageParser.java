package com.lovart.maildesk.integration.gmail;

import com.fasterxml.jackson.databind.JsonNode;
import com.lovart.maildesk.common.exception.GmailIntegrationException;
import com.lovart.maildesk.domain.gmail.GmailFullMessage;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Gmail API {@code messages.get(format=full)} JSON. Ported from legacy
 * {@code lib/gmail/parser.ts}.
 */
final class GmailMessageParser {

    private static final Pattern EMAIL_IN_ANGLE =
            Pattern.compile("<([^>]+@[^>]+)>");
    private static final Pattern PLAIN_EMAIL =
            Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);

    GmailFullMessage parse(JsonNode message) {
        String id = requiredText(message.path("id"), "Gmail message id");
        String threadId = requiredText(message.path("threadId"), "Gmail thread id");
        JsonNode payload = message.path("payload");
        JsonNode headers = payload.path("headers");
        String from = header(headers, "From");
        String to = header(headers, "To");
        String cc = header(headers, "Cc");
        String subject = header(headers, "Subject");
        if (subject.isBlank()) {
            subject = "无主题";
        }
        BodyParts bodies = extractBodies(payload);
        List<String> attachmentNames = extractAttachmentNames(payload);
        OffsetDateTime sentAt = normalizeSentAt(message.path("internalDate").asText(null), header(headers, "Date"));
        List<String> labelIds = new ArrayList<>();
        JsonNode labels = message.path("labelIds");
        if (labels.isArray()) {
            for (JsonNode label : labels) {
                labelIds.add(label.asText());
            }
        }
        return new GmailFullMessage(
                id,
                threadId,
                textOrNull(message.path("historyId")),
                extractEmailAddress(from),
                parseAddressList(to),
                parseAddressList(cc),
                subject,
                bodies.text.isBlank() ? stripHtml(bodies.html) : bodies.text,
                bodies.html,
                attachmentNames,
                !attachmentNames.isEmpty(),
                sentAt,
                labelIds);
    }

    static String getCounterpartyEmail(
            String direction, String fromEmail, List<String> toEmails, List<String> ccEmails, String ownEmail) {
        String own = normalizeEmail(ownEmail);
        String from = normalizeEmail(fromEmail);
        List<String> recipients = new ArrayList<>(toEmails);
        recipients.addAll(ccEmails);
        String firstExternal = recipients.stream()
                .map(GmailMessageParser::normalizeEmail)
                .filter(email -> !email.isBlank() && !email.equals(own))
                .findFirst()
                .orElse(null);
        if ("inbound".equals(direction)) {
            if (!from.equals(own)) {
                return from;
            }
            return firstExternal != null ? firstExternal : from;
        }
        return firstExternal != null ? firstExternal : recipients.stream().findFirst().orElse(from);
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String header(JsonNode headers, String name) {
        if (!headers.isArray()) {
            return "";
        }
        for (JsonNode item : headers) {
            if (name.equalsIgnoreCase(item.path("name").asText(""))) {
                return item.path("value").asText("");
            }
        }
        return "";
    }

    private static String extractEmailAddress(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        Matcher angle = EMAIL_IN_ANGLE.matcher(raw);
        if (angle.find()) {
            return normalizeEmail(angle.group(1));
        }
        Matcher plain = PLAIN_EMAIL.matcher(raw);
        if (plain.find()) {
            return normalizeEmail(plain.group());
        }
        return normalizeEmail(raw);
    }

    private static List<String> parseAddressList(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split(",")) {
            String email = extractEmailAddress(part.trim());
            if (!email.isBlank()) {
                out.add(email);
            }
        }
        return out;
    }

    private static BodyParts extractBodies(JsonNode payload) {
        BodyParts result = new BodyParts();
        if (payload == null || payload.isMissingNode()) {
            return result;
        }
        List<JsonNode> stack = new ArrayList<>();
        stack.add(payload);
        while (!stack.isEmpty()) {
            JsonNode part = stack.remove(0);
            JsonNode parts = part.path("parts");
            if (parts.isArray()) {
                for (JsonNode child : parts) {
                    stack.add(child);
                }
            }
            String mime = part.path("mimeType").asText("");
            String data = part.path("body").path("data").asText(null);
            if (data == null || data.isBlank()) {
                continue;
            }
            String decoded = decodeBase64Url(data);
            if ("text/plain".equalsIgnoreCase(mime)) {
                result.text = decoded;
            } else if ("text/html".equalsIgnoreCase(mime)) {
                result.html = decoded;
            }
        }
        if (result.text.isBlank() && result.html.isBlank()) {
            String data = payload.path("body").path("data").asText(null);
            if (data != null && !data.isBlank()) {
                result.text = decodeBase64Url(data);
            }
        }
        return result;
    }

    private static List<String> extractAttachmentNames(JsonNode payload) {
        List<String> names = new ArrayList<>();
        if (payload == null || payload.isMissingNode()) {
            return names;
        }
        List<JsonNode> stack = new ArrayList<>();
        stack.add(payload);
        while (!stack.isEmpty()) {
            JsonNode part = stack.remove(0);
            JsonNode parts = part.path("parts");
            if (parts.isArray()) {
                for (JsonNode child : parts) {
                    stack.add(child);
                }
            }
            String filename = part.path("filename").asText("");
            if (!filename.isBlank()) {
                names.add(filename);
            }
        }
        return names;
    }

    private static OffsetDateTime normalizeSentAt(String internalDate, String dateHeader) {
        if (internalDate != null && !internalDate.isBlank()) {
            try {
                long millis = Long.parseLong(internalDate);
                return OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        if (dateHeader != null && !dateHeader.isBlank()) {
            try {
                return OffsetDateTime.parse(dateHeader, DateTimeFormatter.RFC_1123_DATE_TIME);
            } catch (DateTimeParseException ignored) {
                // fall through
            }
        }
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private static String decodeBase64Url(String data) {
        byte[] bytes = Base64.getUrlDecoder().decode(data);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private static String requiredText(JsonNode node, String label) {
        String value = textOrNull(node);
        if (value == null) {
            throw new GmailIntegrationException(label + " missing");
        }
        return value;
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        String text = node.asText(null);
        return text == null || text.isBlank() ? null : text;
    }

    private static final class BodyParts {
        private String text = "";
        private String html = "";
    }
}

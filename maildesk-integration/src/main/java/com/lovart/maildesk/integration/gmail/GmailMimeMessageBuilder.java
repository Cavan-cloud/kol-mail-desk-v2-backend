package com.lovart.maildesk.integration.gmail;

import com.lovart.maildesk.domain.gmail.GmailOutboundMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Builds RFC 822 MIME messages and encodes them as Gmail {@code raw} payloads.
 */
final class GmailMimeMessageBuilder {

    private GmailMimeMessageBuilder() {}

    static String buildRaw(GmailOutboundMessage message) {
        List<String> baseHeaders = new ArrayList<>();
        baseHeaders.add("To: " + message.to());
        if (!message.ccEmails().isEmpty()) {
            baseHeaders.add("Cc: " + String.join(", ", message.ccEmails()));
        }
        baseHeaders.add("Subject: " + encodeMimeHeader(message.subject()));
        baseHeaders.add("MIME-Version: 1.0");

        String html = message.bodyHtml();
        if (html != null && !html.isBlank()) {
            String boundary = "kmd_" + UUID.randomUUID().toString().replace("-", "");
            List<String> lines = new ArrayList<>(baseHeaders);
            lines.add("Content-Type: multipart/alternative; boundary=\"" + boundary + "\"");
            lines.add("");
            lines.add("--" + boundary);
            lines.add("Content-Type: text/plain; charset=UTF-8");
            lines.add("Content-Transfer-Encoding: 8bit");
            lines.add("");
            lines.add(message.bodyText());
            lines.add("");
            lines.add("--" + boundary);
            lines.add("Content-Type: text/html; charset=UTF-8");
            lines.add("Content-Transfer-Encoding: 8bit");
            lines.add("");
            lines.add(wrapHtmlDocument(html));
            lines.add("");
            lines.add("--" + boundary + "--");
            lines.add("");
            return toBase64Url(String.join("\r\n", lines));
        }

        List<String> lines = new ArrayList<>(baseHeaders);
        lines.add("Content-Type: text/plain; charset=UTF-8");
        lines.add("Content-Transfer-Encoding: 8bit");
        lines.add("");
        lines.add(message.bodyText());
        return toBase64Url(String.join("\r\n", lines));
    }

    static String wrapHtmlDocument(String bodyHtml) {
        return "<!doctype html><html><head><meta charset=\"utf-8\" /></head>"
                + "<body style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;"
                + "font-size:14px;line-height:1.65;color:#182126;\">"
                + bodyHtml
                + "</body></html>";
    }

    static String encodeMimeHeader(String value) {
        if (value.chars().allMatch(c -> c >= 0x20 && c <= 0x7E)) {
            return value;
        }
        return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)) + "?=";
    }

    static String toBase64Url(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}

package com.lovart.maildesk.integration.gmail;

import com.lovart.maildesk.domain.gmail.GmailOutboundMessage;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GmailMimeMessageBuilderTest {

    @Test
    void buildRaw_usesMultipartAlternativeWhenHtmlPresent() {
        String raw = GmailMimeMessageBuilder.buildRaw(new GmailOutboundMessage(
                "to@example.com",
                List.of("cc@example.com"),
                "测试主题",
                "Plain fallback",
                "<p><strong>Hi</strong></p>"));

        String mime = new String(Base64.getUrlDecoder().decode(raw), StandardCharsets.UTF_8);
        assertThat(mime).contains("To: to@example.com");
        assertThat(mime).contains("Cc: cc@example.com");
        assertThat(mime).contains("Subject: =?UTF-8?B?");
        assertThat(mime).contains("Content-Type: multipart/alternative");
        assertThat(mime).contains("Content-Type: text/plain; charset=UTF-8");
        assertThat(mime).contains("Plain fallback");
        assertThat(mime).contains("Content-Type: text/html; charset=UTF-8");
        assertThat(mime).contains("<p><strong>Hi</strong></p>");
    }

    @Test
    void buildRaw_usesPlainTextWhenHtmlMissing() {
        String raw = GmailMimeMessageBuilder.buildRaw(new GmailOutboundMessage(
                "to@example.com", List.of(), "Hello", "Plain only", null));

        String mime = new String(Base64.getUrlDecoder().decode(raw), StandardCharsets.UTF_8);
        assertThat(mime).contains("Content-Type: text/plain; charset=UTF-8");
        assertThat(mime).doesNotContain("multipart/alternative");
        assertThat(mime).contains("Plain only");
    }
}

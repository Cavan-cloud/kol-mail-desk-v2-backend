package com.lovart.maildesk.integration.gmail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.domain.gmail.GmailFullMessage;
import com.lovart.maildesk.domain.gmail.GmailOutboundMessage;
import com.lovart.maildesk.domain.gmail.GmailSentMessage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real Gmail send + readback smoke test. Skipped unless {@code GMAIL_SMOKE_ACCESS_TOKEN}
 * and {@code GMAIL_SMOKE_TO} are set — not part of default CI (see {@code 06-testing.md} §2.3).
 *
 * <p>Runbook: {@code kol-mail-desk-v2-docs/scripts/gmail-send-smoke.md}
 */
@Tag("gmail-smoke")
@EnabledIf("isGmailSmokeConfigured")
class GmailSendSmokeTest {

    static boolean isGmailSmokeConfigured() {
        return GmailSmokeEnv.isConfigured();
    }

    @Test
    void sendSelfReceive_withCcAndRichText_readbackMatches() {
        GmailClientImpl client = GmailSmokeEnv.buildClient();
        String accessToken = GmailSmokeEnv.accessToken();
        String to = GmailSmokeEnv.toAddress();
        String cc = GmailSmokeEnv.ccAddress();
        String subject = "[MailDesk Smoke] " + Instant.now();
        String plainMarker = "MailDesk smoke plain " + Instant.now().toEpochMilli();
        String htmlMarker = "<p><strong>MailDesk smoke HTML</strong></p>"
                + "<ul><li>bold</li><li>list</li></ul>";

        GmailOutboundMessage outbound = new GmailOutboundMessage(
                to,
                List.of(cc),
                subject,
                plainMarker + " — please ignore.",
                htmlMarker);

        GmailSentMessage sent = client.sendMessage(accessToken, outbound);
        assertThat(sent.messageId()).isNotBlank();

        GmailFullMessage readback = client.getMessage(accessToken, sent.messageId());
        assertThat(readback.subject()).contains("MailDesk Smoke");
        assertThat(readback.toEmails())
                .anyMatch(email -> email.equalsIgnoreCase(to));
        assertThat(readback.ccEmails())
                .anyMatch(email -> email.equalsIgnoreCase(cc));
        assertThat(readback.bodyText()).containsIgnoringCase("MailDesk smoke plain");
        assertThat(readback.bodyHtml()).contains("<strong>MailDesk smoke HTML</strong>");
        assertThat(readback.bodyHtml()).contains("<li>list</li>");
    }
}

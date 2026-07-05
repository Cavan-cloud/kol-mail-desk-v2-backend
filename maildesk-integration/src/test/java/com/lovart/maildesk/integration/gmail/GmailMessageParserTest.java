package com.lovart.maildesk.integration.gmail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GmailMessageParserTest {

    private final GmailMessageParser parser = new GmailMessageParser();

    @Test
    void parse_extractsHeadersAndUnreadLabel() throws Exception {
        var node = new ObjectMapper().readTree("""
                {
                  "id": "abc",
                  "threadId": "t1",
                  "historyId": "42",
                  "labelIds": ["INBOX","UNREAD"],
                  "internalDate": "1700000000000",
                  "payload": {
                    "headers": [
                      {"name":"From","value":"Creator <creator@example.com>"},
                      {"name":"To","value":"me@company.com"},
                      {"name":"Subject","value":"Hello"}
                    ],
                    "mimeType": "text/plain",
                    "body": {"data": "SGVsbG8="}
                  }
                }
                """);

        var message = parser.parse(node);

        assertThat(message.gmailMessageId()).isEqualTo("abc");
        assertThat(message.fromEmail()).isEqualTo("creator@example.com");
        assertThat(message.labelIds()).contains("UNREAD");
        assertThat(message.bodyText()).isEqualTo("Hello");
    }

    @Test
    void getCounterpartyEmail_inboundUsesFrom() {
        String email = GmailMessageParser.getCounterpartyEmail(
                "inbound",
                "creator@example.com",
                java.util.List.of("me@company.com"),
                java.util.List.of(),
                "me@company.com");
        assertThat(email).isEqualTo("creator@example.com");
    }
}

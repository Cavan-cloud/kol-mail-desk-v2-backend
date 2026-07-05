package com.lovart.maildesk.integration.gmail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GmailClientImplTest {

    private GmailProperties properties;
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private GmailClientImpl client;

    @BeforeEach
    void setUp() {
        properties = new GmailProperties("id", "secret", java.time.Duration.ofSeconds(5), java.time.Duration.ofSeconds(5), 1);
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new GmailClientImpl(properties, new ObjectMapper(), restTemplate, new GmailMessageParser());
    }

    @Test
    void listRecentMessages_parsesIdsAndPageToken() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/users/me/messages")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess(
                        """
                        {
                          "messages": [{"id":"m1"},{"id":"m2"}],
                          "nextPageToken": "next-1"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        var page = client.listRecentMessages("access-token", 30, 365, null);
        assertThat(page.messageIds()).containsExactly("m1", "m2");
        assertThat(page.nextPageToken()).isEqualTo("next-1");
        server.verify();
    }

    @Test
    void sendMessage_postsRawMimePayload() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/users/me/messages/send")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess(
                        """
                        {
                          "id": "sent-1",
                          "threadId": "thread-1"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        var sent = client.sendMessage(
                "access-token",
                new com.lovart.maildesk.domain.gmail.GmailOutboundMessage(
                        "to@example.com",
                        java.util.List.of("cc@example.com"),
                        "Subject",
                        "Plain",
                        "<p>Html</p>"));

        assertThat(sent.messageId()).isEqualTo("sent-1");
        assertThat(sent.threadId()).isEqualTo("thread-1");
        server.verify();
    }
}

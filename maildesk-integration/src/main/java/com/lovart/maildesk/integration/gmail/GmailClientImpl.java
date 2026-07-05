package com.lovart.maildesk.integration.gmail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.common.exception.GmailIntegrationException;
import com.lovart.maildesk.domain.gmail.GmailClient;
import com.lovart.maildesk.domain.gmail.GmailFullMessage;
import com.lovart.maildesk.domain.gmail.GmailHistoryPage;
import com.lovart.maildesk.domain.gmail.GmailMessageListPage;
import com.lovart.maildesk.domain.gmail.GmailOutboundMessage;
import com.lovart.maildesk.domain.gmail.GmailSentMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Gmail REST API client (read-only). Ported from legacy {@code lib/gmail/sync.ts}.
 */
@Service
public class GmailClientImpl implements GmailClient {

    private static final String GMAIL_BASE = "https://gmail.googleapis.com/gmail/v1";
    private static final int SAFETY_NET_MAX = 50;

    private final GmailProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GmailMessageParser parser;

    @Autowired
    public GmailClientImpl(GmailProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, buildRestTemplate(properties), new GmailMessageParser());
    }

    GmailClientImpl(
            GmailProperties properties,
            ObjectMapper objectMapper,
            RestTemplate restTemplate,
            GmailMessageParser parser) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.parser = parser;
    }

    @Override
    public GmailMessageListPage listRecentMessages(
            String accessToken, int maxResults, int historyDays, String pageToken) {
        int days = Math.min(Math.max(historyDays, 1), 3650);
        int pageSize = Math.min(Math.max(maxResults, 1), 100);
        String url = UriComponentsBuilder.fromHttpUrl(GMAIL_BASE + "/users/me/messages")
                .queryParam("maxResults", pageSize)
                .queryParam("q", "newer_than:" + days + "d")
                .queryParamIfPresent("pageToken", java.util.Optional.ofNullable(pageToken).filter(s -> !s.isBlank()))
                .build(true)
                .toUriString();
        JsonNode body = getJson(accessToken, url);
        List<String> ids = extractMessageIds(body.path("messages"));
        String next = textOrNull(body.path("nextPageToken"));
        return new GmailMessageListPage(ids, next, "historical_recent");
    }

    @Override
    public GmailHistoryPage listIncrementalMessageIds(
            String accessToken, String startHistoryId, int maxResults) {
        Set<String> ids = new LinkedHashSet<>();
        try {
            String historyUrl = UriComponentsBuilder.fromHttpUrl(GMAIL_BASE + "/users/me/history")
                    .queryParam("startHistoryId", startHistoryId)
                    .queryParam("historyTypes", "messageAdded")
                    .queryParam("maxResults", maxResults)
                    .build(true)
                    .toUriString();
            JsonNode historyBody = getJson(accessToken, historyUrl);
            collectHistoryIds(historyBody.path("history"), ids);

            String safetyUrl = UriComponentsBuilder.fromHttpUrl(GMAIL_BASE + "/users/me/messages")
                    .queryParam("maxResults", SAFETY_NET_MAX)
                    .queryParam("q", "newer_than:2d")
                    .build(true)
                    .toUriString();
            JsonNode safetyBody = getJson(accessToken, safetyUrl);
            ids.addAll(extractMessageIds(safetyBody.path("messages")));
            return new GmailHistoryPage(new ArrayList<>(ids), "history");
        } catch (GmailIntegrationException ex) {
            if (isHistoryIdTooOld(ex)) {
                return new GmailHistoryPage(List.of(), "recent_fallback");
            }
            throw ex;
        }
    }

    @Override
    public GmailFullMessage getMessage(String accessToken, String messageId) {
        String url = GMAIL_BASE + "/users/me/messages/" + messageId + "?format=full";
        try {
            JsonNode body = getJson(accessToken, url);
            return parser.parse(body);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new GmailIntegrationException("Gmail message not found: " + messageId, ex);
        }
    }

    @Override
    public GmailSentMessage sendMessage(String accessToken, GmailOutboundMessage message) {
        String raw = GmailMimeMessageBuilder.buildRaw(message);
        String url = GMAIL_BASE + "/users/me/messages/send";
        return executeWithRetry(() -> {
            HttpHeaders headers = bearerHeaders(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body;
            try {
                body = objectMapper.writeValueAsString(java.util.Map.of("raw", raw));
            } catch (Exception e) {
                throw new GmailIntegrationException("Failed to encode Gmail send payload", e);
            }
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            try {
                JsonNode json = objectMapper.readTree(response.getBody());
                String id = textOrNull(json.path("id"));
                if (id == null) {
                    throw new GmailIntegrationException("Gmail send succeeded but message id is missing");
                }
                String threadId = textOrNull(json.path("threadId"));
                return new GmailSentMessage(id, threadId == null ? id : threadId);
            } catch (GmailIntegrationException ex) {
                throw ex;
            } catch (Exception e) {
                throw new GmailIntegrationException("Failed to parse Gmail send response", e);
            }
        });
    }

    private JsonNode getJson(String accessToken, String url) {
        return executeWithRetry(() -> {
            HttpHeaders headers = bearerHeaders(accessToken);
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            try {
                return objectMapper.readTree(response.getBody());
            } catch (Exception e) {
                throw new GmailIntegrationException("Failed to parse Gmail response", e);
            }
        });
    }

    private static HttpHeaders bearerHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private static List<String> extractMessageIds(JsonNode messagesNode) {
        List<String> ids = new ArrayList<>();
        if (!messagesNode.isArray()) {
            return ids;
        }
        for (JsonNode item : messagesNode) {
            String id = textOrNull(item.path("id"));
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static void collectHistoryIds(JsonNode historyNode, Set<String> ids) {
        if (!historyNode.isArray()) {
            return;
        }
        for (JsonNode item : historyNode) {
            JsonNode added = item.path("messagesAdded");
            if (!added.isArray()) {
                continue;
            }
            for (JsonNode entry : added) {
                String id = textOrNull(entry.path("message").path("id"));
                if (id != null) {
                    ids.add(id);
                }
            }
        }
    }

    private static boolean isHistoryIdTooOld(GmailIntegrationException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof HttpClientErrorException http) {
            return http.getStatusCode().value() == 404;
        }
        return ex.getMessage() != null && ex.getMessage().contains("404");
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        String text = node.asText(null);
        return text == null || text.isBlank() ? null : text;
    }

    private <T> T executeWithRetry(java.util.function.Supplier<T> action) {
        int attempts = Math.max(properties.maxRetries(), 1);
        RuntimeException last = null;
        for (int i = 0; i < attempts; i++) {
            try {
                return action.get();
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                    throw new GmailIntegrationException("Gmail authorization failed", true);
                }
                if (ex.getStatusCode().value() == 404) {
                    throw new GmailIntegrationException("Gmail resource not found", ex);
                }
                last = new GmailIntegrationException("Gmail API error: " + ex.getStatusCode(), ex);
            } catch (RestClientException ex) {
                last = new GmailIntegrationException("Gmail API transport error", ex);
            }
            sleepBackoff(i);
        }
        throw last == null ? new GmailIntegrationException("Gmail API failed") : last;
    }

    private static void sleepBackoff(int attempt) {
        if (attempt <= 0) {
            return;
        }
        try {
            Thread.sleep(Duration.ofMillis(200L * (1L << attempt)).toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static RestTemplate buildRestTemplate(GmailProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.connectTimeout().toMillis());
        factory.setReadTimeout((int) properties.readTimeout().toMillis());
        return new RestTemplate(factory);
    }
}

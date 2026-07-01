package com.lovart.maildesk.integration.feishu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.common.exception.FeishuIntegrationException;
import com.lovart.maildesk.domain.feishu.FeishuBitableRecord;
import com.lovart.maildesk.domain.feishu.FeishuClient;
import com.lovart.maildesk.domain.feishu.FeishuConfigCheckResult;
import com.lovart.maildesk.domain.feishu.FeishuSheetMeta;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Read-only Feishu Open API client (Sheet v2/v3 + Bitable v1). Ported from legacy
 * {@code lib/feishu/sync-kols.ts} HTTP calls; field mapping stays in application layer.
 */
@Service
public class FeishuClientImpl implements FeishuClient {

    private static final int SHEET_VALUE_BATCH_ROWS = 400;
    private static final int BITABLE_PAGE_SIZE = 100;

    private final FeishuProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicReference<CachedToken> tokenCache = new AtomicReference<>();

    public FeishuClientImpl(FeishuProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, buildRestTemplate(properties));
    }

    /** Package-visible for tests with {@link org.springframework.test.web.client.MockRestServiceServer}. */
    FeishuClientImpl(FeishuProperties properties, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean isConfigured() {
        return !properties.getAppId().isBlank()
                && !properties.getAppSecret().isBlank()
                && !properties.getKolAppToken().isBlank();
    }

    @Override
    public String configuredKolAppToken() {
        return properties.getKolAppToken();
    }

    @Override
    public FeishuConfigCheckResult verifyConfiguration() {
        List<String> missing = missingConfigKeys();
        if (!missing.isEmpty()) {
            return FeishuConfigCheckResult.notConfigured(missing);
        }
        String token = properties.getKolAppToken();
        List<FeishuSheetMeta> sheets = listSheets(token);
        String prefix = token.length() <= 8 ? token + "..." : token.substring(0, 8) + "...";
        List<String> titles = sheets.stream().map(FeishuSheetMeta::title).limit(20).toList();
        return FeishuConfigCheckResult.checked(!sheets.isEmpty(), prefix, sheets.size(), titles);
    }

    @Override
    public List<FeishuSheetMeta> listSheets() {
        requireKolAppToken();
        return listSheets(properties.getKolAppToken());
    }

    @Override
    public List<FeishuSheetMeta> listSheets(String spreadsheetToken) {
        String accessToken = tenantAccessToken();
        JsonNode root = executeWithRetry(() -> getForJson(
                "/sheets/v3/spreadsheets/{token}/sheets/query",
                accessToken,
                spreadsheetToken
        ));
        assertFeishuOk(root, "读取飞书电子表格分页失败");
        JsonNode sheets = root.path("data").path("sheets");
        List<FeishuSheetMeta> result = new ArrayList<>();
        if (!sheets.isArray()) {
            return result;
        }
        for (JsonNode sheet : sheets) {
            JsonNode grid = sheet.path("grid_properties");
            result.add(new FeishuSheetMeta(
                    sheet.path("sheet_id").asText(),
                    sheet.path("title").asText(),
                    grid.path("row_count").asInt(0),
                    grid.path("column_count").asInt(0)
            ));
        }
        return result;
    }

    @Override
    public List<List<Object>> readSheetValues(FeishuSheetMeta sheet) {
        requireKolAppToken();
        return readSheetValues(properties.getKolAppToken(), sheet, null);
    }

    @Override
    public List<List<Object>> readSheetValues(String spreadsheetToken, FeishuSheetMeta sheet, Integer maxRows) {
        String accessToken = tenantAccessToken();
        String lastCol = columnLetter(Math.max(1, sheet.columnCount()));
        int cap = sheet.rowCount();
        if (maxRows != null && maxRows > 0) {
            cap = Math.min(cap, maxRows + 1);
        }
        List<List<Object>> rows = new ArrayList<>();
        for (int start = 1; start <= cap; start += SHEET_VALUE_BATCH_ROWS) {
            int end = Math.min(cap, start + SHEET_VALUE_BATCH_ROWS - 1);
            String range = sheet.sheetId() + "!A" + start + ":" + lastCol + end;
            JsonNode root = executeWithRetry(() -> getForJson(
                    "/sheets/v2/spreadsheets/{token}/values/{range}?valueRenderOption=ToString",
                    accessToken,
                    spreadsheetToken,
                    range
            ));
            assertFeishuOk(root, sheet.title() + " 读取失败");
            JsonNode values = root.path("data").path("valueRange").path("values");
            if (values.isArray()) {
                for (JsonNode row : values) {
                    rows.add(jsonRowToObjects(row));
                }
            }
        }
        return rows;
    }

    @Override
    public List<FeishuBitableRecord> listBitableRecords(String appToken, String tableId, Integer maxRecords) {
        String accessToken = tenantAccessToken();
        List<FeishuBitableRecord> records = new ArrayList<>();
        String pageToken = "";
        do {
            final String nextPage = pageToken;
            JsonNode root = executeWithRetry(() -> {
                String path = "/bitable/v1/apps/{app}/tables/{table}/records?page_size=" + BITABLE_PAGE_SIZE;
                if (!nextPage.isBlank()) {
                    path += "&page_token=" + nextPage;
                }
                return getForJson(path, accessToken, appToken, tableId);
            });
            assertFeishuOk(root, "读取飞书多维表格失败");
            JsonNode items = root.path("data").path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    if (maxRecords != null && records.size() >= maxRecords) {
                        return records;
                    }
                    records.add(toBitableRecord(item));
                }
            }
            if (maxRecords != null && records.size() >= maxRecords) {
                break;
            }
            boolean hasMore = root.path("data").path("has_more").asBoolean(false);
            pageToken = hasMore ? root.path("data").path("page_token").asText("") : "";
        } while (!pageToken.isBlank());
        return records;
    }

    @Override
    public List<FeishuBitableRecord> listBitableRecords(Integer maxRecords) {
        requireKolAppToken();
        requireKolTableId();
        return listBitableRecords(properties.getKolAppToken(), properties.getKolTableId(), maxRecords);
    }

    private String getForJson(String pathTemplate, String accessToken, Object... uriVariables) {
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl(pathTemplate),
                HttpMethod.GET,
                authEntity(accessToken),
                String.class,
                uriVariables
        );
        return response.getBody();
    }

    private List<String> missingConfigKeys() {
        List<String> missing = new ArrayList<>();
        if (properties.getAppId().isBlank()) {
            missing.add("FEISHU_APP_ID");
        }
        if (properties.getAppSecret().isBlank()) {
            missing.add("FEISHU_APP_SECRET");
        }
        if (properties.getKolAppToken().isBlank()) {
            missing.add("FEISHU_KOL_APP_TOKEN");
        }
        return missing;
    }

    private void requireKolAppToken() {
        if (properties.getKolAppToken().isBlank()) {
            throw new FeishuIntegrationException("FEISHU_KOL_APP_TOKEN is not configured");
        }
    }

    private void requireKolTableId() {
        if (properties.getKolTableId().isBlank()) {
            throw new FeishuIntegrationException("FEISHU_KOL_TABLE_ID is not configured");
        }
    }

    private String tenantAccessToken() {
        CachedToken cached = tokenCache.get();
        if (cached != null && cached.isValid()) {
            return cached.token();
        }
        synchronized (this) {
            cached = tokenCache.get();
            if (cached != null && cached.isValid()) {
                return cached.token();
            }
            JsonNode root = executeWithRetry(() -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                        Map.of(
                                "app_id", properties.getAppId(),
                                "app_secret", properties.getAppSecret()
                        ),
                        headers
                );
                ResponseEntity<String> response = restTemplate.exchange(
                        apiUrl("/auth/v3/tenant_access_token/internal"),
                        HttpMethod.POST,
                        entity,
                        String.class
                );
                return response.getBody();
            });
            assertFeishuOk(root, "获取飞书 tenant_access_token 失败");
            String token = root.path("tenant_access_token").asText(null);
            if (token == null || token.isBlank()) {
                throw new FeishuIntegrationException("获取飞书 tenant_access_token 失败");
            }
            int expireSeconds = root.path("expire").asInt(7200);
            Instant expiresAt = Instant.now().plusSeconds(Math.max(60, expireSeconds - 300L));
            tokenCache.set(new CachedToken(token, expiresAt));
            return token;
        }
    }

    private HttpEntity<Void> authEntity(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, bearer(accessToken));
        return new HttpEntity<>(headers);
    }

    private FeishuBitableRecord toBitableRecord(JsonNode item) {
        String recordId = item.path("record_id").asText();
        Map<String, Object> fields = new HashMap<>();
        JsonNode fieldNode = item.path("fields");
        if (fieldNode.isObject()) {
            fieldNode.fields().forEachRemaining(entry ->
                    fields.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class)));
        }
        return new FeishuBitableRecord(recordId, Map.copyOf(fields));
    }

    private List<Object> jsonRowToObjects(JsonNode row) {
        List<Object> cells = new ArrayList<>();
        if (!row.isArray()) {
            return cells;
        }
        for (JsonNode cell : row) {
            if (cell.isNull()) {
                cells.add(null);
            } else if (cell.isValueNode()) {
                cells.add(cell.asText());
            } else {
                cells.add(objectMapper.convertValue(cell, Object.class));
            }
        }
        return cells;
    }

    private JsonNode parseJson(String body) {
        if (body == null || body.isBlank()) {
            throw new FeishuIntegrationException("Empty Feishu response body");
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new FeishuIntegrationException("Invalid Feishu JSON response", e);
        }
    }

    private void assertFeishuOk(JsonNode root, String fallbackMessage) {
        int code = root.path("code").asInt(-1);
        if (code != 0) {
            String msg = root.path("msg").asText(fallbackMessage);
            throw new FeishuIntegrationException(code, msg);
        }
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String apiUrl(String pathAndQuery) {
        String base = properties.getApiBase();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + pathAndQuery;
    }

    private static RestTemplate buildRestTemplate(FeishuProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) props.getConnectTimeout().toMillis());
        factory.setReadTimeout((int) props.getReadTimeout().toMillis());
        return new RestTemplate(factory);
    }

    private interface RestCall {
        String execute() throws RestClientException;
    }

    private JsonNode executeWithRetry(RestCall call) {
        int attempts = Math.max(1, properties.getMaxRetries());
        RestClientException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return parseJson(call.execute());
            } catch (RestClientException ex) {
                last = ex;
                if (attempt >= attempts) {
                    break;
                }
                sleepBackoff(attempt);
            }
        }
        throw new FeishuIntegrationException("Feishu API request failed after retries", last);
    }

    private static void sleepBackoff(int attempt) {
        try {
            Thread.sleep(Duration.ofMillis(200L * attempt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FeishuIntegrationException("Feishu API retry interrupted", e);
        }
    }

    /** Excel-style column letter for 1-based column index (1 → A, 27 → AA). */
    static String columnLetter(int columnCount) {
        int n = columnCount;
        StringBuilder s = new StringBuilder();
        while (n > 0) {
            int m = (n - 1) % 26;
            s.insert(0, (char) ('A' + m));
            n = (n - 1) / 26;
        }
        return s.isEmpty() ? "A" : s.toString();
    }

    private record CachedToken(String token, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}

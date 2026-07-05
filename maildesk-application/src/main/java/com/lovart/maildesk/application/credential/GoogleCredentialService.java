package com.lovart.maildesk.application.credential;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.common.exception.GmailIntegrationException;
import com.lovart.maildesk.domain.credential.GoogleAccessToken;
import com.lovart.maildesk.domain.credential.GoogleCredentialPort;
import com.lovart.maildesk.domain.credential.entity.IntegrationCredentialDO;
import com.lovart.maildesk.domain.credential.mapper.IntegrationCredentialMapper;
import com.lovart.maildesk.domain.crypto.TokenEncryptionPort;
import com.lovart.maildesk.integration.gmail.GmailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoogleCredentialService implements GoogleCredentialPort {

    private static final Logger log = LoggerFactory.getLogger(GoogleCredentialService.class);
    private static final String CREDENTIAL_TYPE = "google";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final Duration SKEW = Duration.ofMinutes(2);

    private final IntegrationCredentialMapper credentials;
    private final TokenEncryptionPort encryption;
    private final GmailProperties gmailProperties;
    private final ObjectMapper mapper;
    private final RestTemplate restTemplate;

    @Autowired
    public GoogleCredentialService(
            IntegrationCredentialMapper credentials,
            TokenEncryptionPort encryption,
            GmailProperties gmailProperties,
            ObjectMapper mapper) {
        this(credentials, encryption, gmailProperties, mapper, new RestTemplate());
    }

    GoogleCredentialService(
            IntegrationCredentialMapper credentials,
            TokenEncryptionPort encryption,
            GmailProperties gmailProperties,
            ObjectMapper mapper,
            RestTemplate restTemplate) {
        this.credentials = credentials;
        this.encryption = encryption;
        this.gmailProperties = gmailProperties;
        this.mapper = mapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean hasCredential(UUID userId) {
        return credentials.selectCount(
                        new LambdaQueryWrapper<IntegrationCredentialDO>()
                                .eq(IntegrationCredentialDO::getUserId, userId)
                                .eq(IntegrationCredentialDO::getType, CREDENTIAL_TYPE))
                > 0;
    }

    @Override
    public boolean hasGmailSendScope(UUID userId) {
        return readStoredScope(userId).contains("gmail.send");
    }

    private String readStoredScope(UUID userId) {
        IntegrationCredentialDO row = credentials.selectOne(
                new LambdaQueryWrapper<IntegrationCredentialDO>()
                        .eq(IntegrationCredentialDO::getUserId, userId)
                        .eq(IntegrationCredentialDO::getType, CREDENTIAL_TYPE));
        if (row == null || row.getEncryptedPayload() == null) {
            return "";
        }
        try {
            JsonNode payload = mapper.readTree(encryption.decrypt(row.getEncryptedPayload()));
            String scope = text(payload, "scope");
            return scope == null ? "" : scope;
        } catch (Exception ex) {
            log.warn("Failed to read Google OAuth scope for user {}: {}", userId, ex.getClass().getSimpleName());
            return "";
        }
    }

    @Override
    public Optional<GoogleAccessToken> resolveAccessToken(UUID userId) {
        IntegrationCredentialDO row = credentials.selectOne(
                new LambdaQueryWrapper<IntegrationCredentialDO>()
                        .eq(IntegrationCredentialDO::getUserId, userId)
                        .eq(IntegrationCredentialDO::getType, CREDENTIAL_TYPE));
        if (row == null || row.getEncryptedPayload() == null) {
            return Optional.empty();
        }
        try {
            JsonNode payload = mapper.readTree(encryption.decrypt(row.getEncryptedPayload()));
            String accessToken = text(payload, "access_token");
            String refreshToken = text(payload, "refresh_token");
            if (accessToken == null || accessToken.isBlank()) {
                return Optional.empty();
            }
            if (!isExpired(payload, row.getExpiresAt())) {
                return Optional.of(new GoogleAccessToken(accessToken));
            }
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new GmailIntegrationException(
                        "未找到 Gmail refresh token，请重新登录并授权 Gmail 权限。", true);
            }
            if (!gmailProperties.oauthConfigured()) {
                throw new GmailIntegrationException("Google OAuth 客户端未配置");
            }
            JsonNode refreshed = refreshAccessToken(refreshToken);
            String newAccess = text(refreshed, "access_token");
            if (newAccess == null || newAccess.isBlank()) {
                throw new GmailIntegrationException("Google token 刷新失败", true);
            }
            Map<String, Object> merged = new LinkedHashMap<>();
            payload.fields().forEachRemaining(e -> merged.put(e.getKey(), e.getValue().asText()));
            merged.put("access_token", newAccess);
            if (refreshed.has("expires_in")) {
                Instant expires = Instant.now().plusSeconds(refreshed.path("expires_in").asLong(3600));
                merged.put("expires_at", expires.toString());
                row.setExpiresAt(OffsetDateTime.ofInstant(expires, ZoneOffset.UTC));
            }
            ObjectMapper compact = mapper.copy().setSerializationInclusion(JsonInclude.Include.NON_NULL);
            row.setEncryptedPayload(encryption.encrypt(compact.writeValueAsString(merged)));
            credentials.updateById(row);
            return Optional.of(new GoogleAccessToken(newAccess));
        } catch (GmailIntegrationException ex) {
            throw ex;
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 400 || ex.getStatusCode().value() == 401) {
                throw new GmailIntegrationException("Gmail refresh token 已失效，请重新授权。", true);
            }
            throw new GmailIntegrationException("Google token 刷新失败", ex);
        } catch (Exception ex) {
            log.warn("Failed to resolve Google credential for user {}: {}", userId, ex.getClass().getSimpleName());
            throw new GmailIntegrationException("无法读取 Gmail 授权信息");
        }
    }

    private JsonNode refreshAccessToken(String refreshToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        form.add("client_id", gmailProperties.clientId());
        form.add("client_secret", gmailProperties.clientSecret());
        ResponseEntity<String> response =
                restTemplate.postForEntity(TOKEN_URL, new HttpEntity<>(form, headers), String.class);
        return mapper.readTree(response.getBody());
    }

    private static boolean isExpired(JsonNode payload, OffsetDateTime expiresAtColumn) {
        Instant now = Instant.now().plus(SKEW);
        String expiresAtText = text(payload, "expires_at");
        if (expiresAtText != null) {
            try {
                return Instant.parse(expiresAtText).isBefore(now);
            } catch (Exception ignored) {
                // fall through
            }
        }
        if (expiresAtColumn != null) {
            return expiresAtColumn.toInstant().isBefore(now);
        }
        return false;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return text == null || text.isBlank() ? null : text;
    }
}

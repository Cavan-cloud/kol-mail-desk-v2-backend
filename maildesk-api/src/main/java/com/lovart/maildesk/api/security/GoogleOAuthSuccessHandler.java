package com.lovart.maildesk.api.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.common.context.TenantContext;
import com.lovart.maildesk.domain.credential.entity.IntegrationCredentialDO;
import com.lovart.maildesk.domain.credential.mapper.IntegrationCredentialMapper;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import com.lovart.maildesk.infrastructure.crypto.TokenEncryptionService;
import com.lovart.maildesk.infrastructure.session.SessionInfo;
import com.lovart.maildesk.infrastructure.session.SessionTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OAuth2 login success → upsert {@code profiles}, AES-encrypt the
 * Google tokens into {@code integration_credentials}, mint an opaque session
 * token, set the {@code MAILDESK_SESSION} cookie, and redirect to the web app.
 * <p>
 * Failures during credential persistence DO NOT block login — the user still
 * gets a session, but the next Gmail call will see no creds and prompt for
 * re-authorization (P3 / F-AUTH-04). We never log raw tokens.
 */
@Component
public class GoogleOAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthSuccessHandler.class);
    private static final String CREDENTIAL_TYPE = "google";

    private final ProfileMapper profiles;
    private final IntegrationCredentialMapper credentials;
    private final OAuth2AuthorizedClientService authorizedClients;
    private final TokenEncryptionService encryption;
    private final SessionTokenService sessionTokens;
    private final ObjectMapper mapper;
    private final UUID defaultTenantId;
    private final String webRedirectUrl;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public GoogleOAuthSuccessHandler(
            ProfileMapper profiles,
            IntegrationCredentialMapper credentials,
            OAuth2AuthorizedClientService authorizedClients,
            TokenEncryptionService encryption,
            SessionTokenService sessionTokens,
            ObjectMapper mapper,
            @Value("${maildesk.default-tenant-id:00000000-0000-0000-0000-000000000001}") String defaultTenantId,
            @Value("${maildesk.web.redirect-url:http://localhost:3000/}") String webRedirectUrl,
            @Value("${maildesk.session.cookie-secure:false}") boolean cookieSecure,
            @Value("${maildesk.session.cookie-same-site:Lax}") String cookieSameSite
    ) {
        this.profiles = profiles;
        this.credentials = credentials;
        this.authorizedClients = authorizedClients;
        this.encryption = encryption;
        this.sessionTokens = sessionTokens;
        this.mapper = mapper;
        this.defaultTenantId = UUID.fromString(defaultTenantId);
        this.webRedirectUrl = webRedirectUrl;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth)) {
            getRedirectStrategy().sendRedirect(request, response, webRedirectUrl);
            return;
        }
        OAuth2User user = oauth.getPrincipal();
        String googleSub = (String) user.getAttributes().get("sub");
        String email = (String) user.getAttributes().get("email");
        String name = stringAttr(user, "name", email);

        TenantContext.setTenantId(defaultTenantId);
        try {
            ProfileDO profile = upsertProfile(googleSub, email, name);
            persistGoogleCredentials(oauth, profile);
            SessionInfo session = sessionTokens.create(profile);
            response.addHeader("Set-Cookie", buildCookie(session.token(), sessionTokens.ttl().toSeconds()));
            getRedirectStrategy().sendRedirect(request, response, webRedirectUrl);
        } finally {
            TenantContext.clear();
        }
    }

    private ProfileDO upsertProfile(String googleSub, String email, String displayName) {
        ProfileDO existing = profiles.selectOne(
                new LambdaQueryWrapper<ProfileDO>().eq(ProfileDO::getGoogleSub, googleSub));
        if (existing != null) {
            existing.setEmail(email);
            existing.setDisplayName(displayName);
            profiles.updateById(existing);
            return existing;
        }
        ProfileDO row = new ProfileDO();
        row.setGoogleSub(googleSub);
        row.setEmail(email);
        row.setDisplayName(displayName);
        row.setRole("member");
        row.setStatus("pending_approval");
        profiles.insert(row);
        return row;
    }

    private void persistGoogleCredentials(OAuth2AuthenticationToken oauth, ProfileDO profile) {
        OAuth2AuthorizedClient client = authorizedClients.loadAuthorizedClient(
                oauth.getAuthorizedClientRegistrationId(), oauth.getName());
        if (client == null) {
            log.warn("OAuth2AuthorizedClient missing for user {}; gmail integration will require re-authorization.",
                    profile.getId());
            return;
        }
        OAuth2AccessToken access = client.getAccessToken();
        OAuth2RefreshToken refresh = client.getRefreshToken();
        if (refresh == null) {
            log.warn("Google returned no refresh_token for user {} — likely a re-consent without prompt=consent. "
                    + "User will need to re-authorize Gmail later.", profile.getId());
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("token_type", access.getTokenType().getValue());
        payload.put("access_token", access.getTokenValue());
        if (access.getExpiresAt() != null) payload.put("expires_at", access.getExpiresAt().toString());
        if (access.getScopes() != null) payload.put("scope", String.join(" ", access.getScopes()));
        if (refresh != null) payload.put("refresh_token", refresh.getTokenValue());

        byte[] sealed;
        try {
            ObjectMapper compact = mapper.copy().setSerializationInclusion(JsonInclude.Include.NON_NULL);
            sealed = encryption.encrypt(compact.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to serialize Google credentials for user {}: {}",
                    profile.getId(), e.getClass().getSimpleName());
            return;
        }

        IntegrationCredentialDO existing = credentials.selectOne(
                new LambdaQueryWrapper<IntegrationCredentialDO>()
                        .eq(IntegrationCredentialDO::getUserId, profile.getId())
                        .eq(IntegrationCredentialDO::getType, CREDENTIAL_TYPE));
        OffsetDateTime expiresAt = access.getExpiresAt() == null
                ? null
                : OffsetDateTime.ofInstant(access.getExpiresAt(), ZoneOffset.UTC);
        if (existing != null) {
            existing.setEncryptedPayload(sealed);
            existing.setExpiresAt(expiresAt);
            credentials.updateById(existing);
            return;
        }
        IntegrationCredentialDO row = new IntegrationCredentialDO();
        row.setUserId(profile.getId());
        row.setType(CREDENTIAL_TYPE);
        row.setEncryptedPayload(sealed);
        row.setExpiresAt(expiresAt);
        credentials.insert(row);
    }

    private String buildCookie(String token, long maxAgeSeconds) {
        StringBuilder sb = new StringBuilder();
        sb.append(SessionTokenService.COOKIE_NAME).append('=').append(token);
        sb.append("; Path=/");
        sb.append("; Max-Age=").append(maxAgeSeconds);
        sb.append("; HttpOnly");
        sb.append("; SameSite=").append(cookieSameSite);
        if (cookieSecure) sb.append("; Secure");
        return sb.toString();
    }

    private static String stringAttr(OAuth2User user, String key, String fallback) {
        Object v = user.getAttributes().get(key);
        return v == null ? fallback : v.toString();
    }

    /** Visible for tests. */
    public Instant nowEpoch() { return Instant.now(); }
}

package com.lovart.maildesk.infrastructure.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues + validates opaque session tokens backed by Redis.
 * <p>
 * Token format: 32 random bytes, base64-url, no padding (43 chars). Redis
 * key {@code maildesk:sess:{token}} holds a small JSON document; TTL is
 * 7 days and is sliding — every successful lookup re-arms the expiry.
 * Tokens are opaque to the client; only the cookie value travels.
 */
@Component
public class SessionTokenService {

    /** Cookie + Redis key namespace. */
    public static final String COOKIE_NAME = "MAILDESK_SESSION";
    private static final String KEY_PREFIX = "maildesk:sess:";
    private static final int TOKEN_BYTES = 32;

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    public SessionTokenService(
            StringRedisTemplate redis,
            @Value("${maildesk.session.ttl-seconds:604800}") long ttlSeconds
    ) {
        this.redis = Objects.requireNonNull(redis, "redis");
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * Issue a brand new session for {@code profile} and persist it in Redis.
     */
    public SessionInfo create(ProfileDO profile) {
        Objects.requireNonNull(profile, "profile");
        String token = newToken();
        OffsetDateTime now = OffsetDateTime.now();
        SessionInfo info = new SessionInfo(
                token,
                profile.getId(),
                profile.getTenantId(),
                profile.getEmail(),
                profile.getDisplayName(),
                profile.getRole(),
                now,
                now.plus(ttl)
        );
        persist(info);
        return info;
    }

    /**
     * Look up a session; returns empty if expired / unknown / corrupted.
     * Sliding TTL: every successful lookup re-arms the expiry.
     */
    public Optional<SessionInfo> find(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        String raw = redis.opsForValue().get(KEY_PREFIX + token);
        if (raw == null) return Optional.empty();
        try {
            SessionInfo info = mapper.readValue(raw, SessionInfo.class);
            // sliding refresh
            redis.expire(KEY_PREFIX + token, ttl);
            return Optional.of(info);
        } catch (Exception e) {
            redis.delete(KEY_PREFIX + token);
            return Optional.empty();
        }
    }

    /** Drop the session (logout). */
    public void invalidate(String token) {
        if (token == null || token.isBlank()) return;
        redis.delete(KEY_PREFIX + token);
    }

    /** Visible for tests. */
    public Duration ttl() { return ttl; }

    private void persist(SessionInfo info) {
        try {
            redis.opsForValue().set(KEY_PREFIX + info.token(), mapper.writeValueAsString(info), ttl);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist session to Redis", e);
        }
    }

    private String newToken() {
        byte[] buf = new byte[TOKEN_BYTES];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}

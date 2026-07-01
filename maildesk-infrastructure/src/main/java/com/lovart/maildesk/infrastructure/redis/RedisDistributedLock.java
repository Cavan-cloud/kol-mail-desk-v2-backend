package com.lovart.maildesk.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Simple Redis lock via {@code SET key token NX EX ttl}. Release uses a Lua
 * compare-and-del so only the holder can delete the key.
 */
@Component
public class RedisDistributedLock {

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                      return redis.call('del', KEYS[1])
                    else
                      return 0
                    end
                    """,
            Long.class);

    private final StringRedisTemplate redis;

    public RedisDistributedLock(StringRedisTemplate redis) {
        this.redis = Objects.requireNonNull(redis, "redis");
    }

    /**
     * @return a handle when the lock was acquired; empty when another holder owns the key
     */
    public Optional<Handle> tryAcquire(String key, Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }

        String token = UUID.randomUUID().toString();
        Boolean acquired = redis.opsForValue().setIfAbsent(key, token, ttl);
        if (Boolean.TRUE.equals(acquired)) {
            return Optional.of(new Handle(key, token));
        }
        return Optional.empty();
    }

    public void release(Handle handle) {
        Objects.requireNonNull(handle, "handle");
        redis.execute(RELEASE_SCRIPT, List.of(handle.key()), handle.token());
    }

    public record Handle(String key, String token) {
    }
}

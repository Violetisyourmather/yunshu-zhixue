package com.yunshu.zhixue.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * 令牌黑名单（04 §2.2）：登出后 accessToken 的 jti 写入
 * {@code token:blacklist:{jti}}，TTL 至令牌过期。
 */
@Component
public class TokenBlacklist {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklist.class);
    private static final String KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redis;

    public TokenBlacklist(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void revoke(String jti, Instant expiresAt) {
        long ttlSeconds = Duration.between(Instant.now(), expiresAt).toSeconds();
        if (ttlSeconds <= 0) {
            return;
        }
        redis.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
    }

    /**
     * Redis 不可用时降级为「不拦截」并告警：签名本身仍然有效，
     * 避免缓存故障导致全部用户无法访问。
     */
    public boolean isRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + jti));
        } catch (DataAccessException ex) {
            log.warn("令牌黑名单查询失败，降级放行：{}", ex.getMessage());
            return false;
        }
    }
}

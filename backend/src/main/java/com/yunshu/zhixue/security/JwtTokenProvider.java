package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT 签发与校验。两端密钥独立，校验时必须显式指定令牌域与令牌类型。
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_PERMS = "perms";
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties properties;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
    }

    public String createAccessToken(Audience audience, long userId, Collection<String> permissions) {
        JwtProperties.Token config = configOf(audience);
        return build(audience, userId, TokenType.ACCESS, permissions, config.getAccessTtl());
    }

    public String createRefreshToken(Audience audience, long userId) {
        JwtProperties.Token config = configOf(audience);
        return build(audience, userId, TokenType.REFRESH, Set.of(), config.getRefreshTtl());
    }

    /**
     * 校验签名、令牌域、类型并解析载荷。
     *
     * @throws BizException 10001（凭证无效或过期）
     */
    public JwtPayload parse(String token, Audience expectedAudience, TokenType expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey(expectedAudience))
                    .requireAudience(expectedAudience.value())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!expectedType.value().equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new BizException(ErrorCode.UNAUTHORIZED);
            }
            Date expiration = claims.getExpiration();
            if (expiration == null) {
                throw new BizException(ErrorCode.UNAUTHORIZED);
            }
            List<?> rawPermissions = claims.get(CLAIM_PERMS, List.class);
            Set<String> permissions = rawPermissions == null
                    ? Set.of()
                    : rawPermissions.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet());

            return new JwtPayload(
                    Long.parseLong(claims.getSubject()),
                    expectedAudience,
                    claims.getId(),
                    expiration.toInstant(),
                    permissions);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String build(Audience audience, long userId, TokenType type,
                         Collection<String> permissions, Duration ttl) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(Long.toString(userId))
                .id(UUID.randomUUID().toString())
                .audience().add(audience.value()).and()
                .claim(CLAIM_TYPE, type.value())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)));
        if (permissions != null && !permissions.isEmpty()) {
            builder.claim(CLAIM_PERMS, permissions);
        }
        return builder.signWith(secretKey(audience)).compact();
    }

    private SecretKey secretKey(Audience audience) {
        String secret = configOf(audience).getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt." + audience.value() + ".secret 未配置或长度不足 " + MIN_SECRET_BYTES + " 字节");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private JwtProperties.Token configOf(Audience audience) {
        return properties.of(audience);
    }
}

package com.yunshu.zhixue.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 双端 JWT 配置（04 §2.2）：access 2h、refresh 14d。
 *
 * <p>两端使用不同 secret，实现令牌不互通；secret 至少 32 字节（HS256）。</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private Token portal = new Token();
    private Token admin = new Token();

    public Token of(Audience audience) {
        return audience == Audience.ADMIN ? admin : portal;
    }

    @Getter
    @Setter
    public static class Token {
        /** HS256 密钥，至少 32 字节；由环境变量注入，不得提交真实值。 */
        private String secret;
        private Duration accessTtl = Duration.ofHours(2);
        private Duration refreshTtl = Duration.ofDays(14);
    }
}

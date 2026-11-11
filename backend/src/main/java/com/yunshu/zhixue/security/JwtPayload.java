package com.yunshu.zhixue.security;

import java.time.Instant;
import java.util.Set;

/**
 * 已校验的令牌载荷。
 *
 * @param userId      用户 id（学员或后台用户）
 * @param audience    令牌域
 * @param jti         令牌唯一 id，用于登出黑名单
 * @param expiresAt   过期时间
 * @param permissions 管理端权限码集合（学员端为空）
 */
public record JwtPayload(
        long userId,
        Audience audience,
        String jti,
        Instant expiresAt,
        Set<String> permissions) {
}

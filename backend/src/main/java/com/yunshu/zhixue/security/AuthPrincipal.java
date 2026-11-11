package com.yunshu.zhixue.security;

import java.util.Set;

/**
 * 放入 SecurityContext 的当前登录主体。
 *
 * @param userId      用户 id
 * @param audience    portal / admin
 * @param permissions 管理端权限码（如 {@code question:edit}），学员端为空集合
 */
public record AuthPrincipal(long userId, Audience audience, Set<String> permissions) {
}

package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户读取工具。业务代码不直接接触 SecurityContext。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthPrincipal principal() {
        AuthPrincipal principal = principalOrNull();
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return principal;
    }

    /** 公开接口需要按登录态裁剪返回值时使用；未登录返回 null。 */
    public static AuthPrincipal principalOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            return null;
        }
        return principal;
    }

    /** 当前登录用户 id；未登录返回 null（仅用于公开接口的可选登录态判断）。 */
    public static Long currentUserIdOrNull() {
        AuthPrincipal principal = principalOrNull();
        return principal == null ? null : principal.userId();
    }

    public static long currentUserId() {
        return principal().userId();
    }

    public static Audience currentAudience() {
        return principal().audience();
    }

    public static boolean isAdmin() {
        return principal().audience() == Audience.ADMIN;
    }
}

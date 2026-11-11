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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return principal;
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

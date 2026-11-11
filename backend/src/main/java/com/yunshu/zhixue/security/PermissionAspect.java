package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * RBAC 权限点校验（04 §2.8）：无权限返回 403 + 10002。
 */
@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requirePermission)")
    public void check(RequirePermission requirePermission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.audience() != Audience.ADMIN) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        String required = JwtAuthenticationFilter.PERMISSION_PREFIX + requirePermission.value();
        boolean granted = authentication.getAuthorities().stream()
                .anyMatch(authority -> required.equals(authority.getAuthority()));
        if (!granted) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }
}

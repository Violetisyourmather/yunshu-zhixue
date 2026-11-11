package com.yunshu.zhixue.modules.system.dto;

import java.time.LocalDateTime;

/**
 * A-M01 用户列表行。
 */
public record UserItem(
        Long id,
        String username,
        String mobile,
        String email,
        Long roleId,
        String roleName,
        String roleCode,
        Integer status,
        boolean builtin,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt) {
}

package com.yunshu.zhixue.modules.system.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A-M08 用户详情（资料 + 最近操作）。
 */
public record UserDetail(
        Long id,
        String username,
        String mobile,
        String email,
        Long roleId,
        String roleName,
        String roleCode,
        Integer status,
        boolean builtin,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        List<RecentOperation> recentOperations) {
}

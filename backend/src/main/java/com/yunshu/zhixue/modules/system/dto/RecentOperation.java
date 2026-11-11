package com.yunshu.zhixue.modules.system.dto;

import java.time.LocalDateTime;

/**
 * 用户详情页「最近操作」条目。
 */
public record RecentOperation(
        Long id,
        String action,
        String targetType,
        String targetName,
        String ip,
        LocalDateTime createdAt) {
}

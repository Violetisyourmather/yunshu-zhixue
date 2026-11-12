package com.yunshu.zhixue.modules.stats.dto;

import java.time.LocalDateTime;

/**
 * A-D04 最近活动行（来自 operation_log，不缓存）。
 */
public record RecentActivityItem(
        long id,
        String username,
        String action,
        String targetType,
        String targetName,
        String ip,
        LocalDateTime createdAt) {
}

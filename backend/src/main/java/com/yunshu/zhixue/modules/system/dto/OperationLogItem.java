package com.yunshu.zhixue.modules.system.dto;

import java.time.LocalDateTime;

/**
 * A-G01 / A-G02 操作日志行与详情。
 */
public record OperationLogItem(
        Long id,
        Long userId,
        String username,
        String action,
        String targetType,
        String targetName,
        String ip,
        LocalDateTime createdAt) {
}

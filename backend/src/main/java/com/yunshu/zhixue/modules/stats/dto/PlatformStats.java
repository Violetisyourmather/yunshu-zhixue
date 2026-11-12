package com.yunshu.zhixue.modules.stats.dto;

/**
 * P-H01 首页平台数据（公开）。
 *
 * @param studentCount   未删除学员数
 * @param questionCount  已上架面试题数（首页「精选面试题」）
 * @param interviewCount 已完成面试场次
 */
public record PlatformStats(
        long studentCount,
        long questionCount,
        long interviewCount) {
}

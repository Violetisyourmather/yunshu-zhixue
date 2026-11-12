package com.yunshu.zhixue.modules.stats.dto;

/**
 * P-H03 学习统计。
 *
 * @param consecutiveDays    连续学习天数（截至今天或昨天）
 * @param accuracyRate       客观题正确率（0~100，保留 1 位小数）
 * @param totalPracticeCount 累计作答记录数
 */
public record LearningStats(
        long consecutiveDays,
        double accuracyRate,
        long totalPracticeCount) {
}

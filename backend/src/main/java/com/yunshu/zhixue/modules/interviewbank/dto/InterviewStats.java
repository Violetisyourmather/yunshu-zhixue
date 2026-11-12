package com.yunshu.zhixue.modules.interviewbank.dto;

/**
 * 面试题库统计卡：总数 / 方向数 / 未分类 / 本日新增。
 */
public record InterviewStats(
        long total,
        long directionCount,
        long unclassifiedCount,
        long todayAdded) {
}

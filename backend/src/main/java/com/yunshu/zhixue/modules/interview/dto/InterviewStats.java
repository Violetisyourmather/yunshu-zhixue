package com.yunshu.zhixue.modules.interview.dto;

/**
 * P-I08 面试统计，分数口径 0~10。
 */
public record InterviewStats(
        long interviewTotal,
        Double avgScore,
        Double maxScore,
        long totalDuration) {
}

package com.yunshu.zhixue.modules.ai.dto;

/**
 * 生成面试报告时使用的单题快照。
 */
public record InterviewQaSnapshot(
        int order,
        String question,
        String referenceAnswer,
        String studentAnswer,
        double score,
        String feedback) {
}

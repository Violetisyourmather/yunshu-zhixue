package com.yunshu.zhixue.modules.interview.dto;

/**
 * 报告生成与落库使用的单题最终数据。
 */
public record InterviewAnswerData(
        int order,
        long questionId,
        String questionText,
        String studentAnswer,
        int durationSeconds,
        double score,
        String feedback) {
}

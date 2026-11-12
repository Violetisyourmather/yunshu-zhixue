package com.yunshu.zhixue.modules.interview.dto;

/**
 * P-I05 面试回放单题。
 */
public record InterviewQuestionReview(
        int order,
        String questionText,
        String studentAnswer,
        double score,
        String feedback) {
}

package com.yunshu.zhixue.modules.interview.dto;

/**
 * P-I02 返回的面试题目，不包含参考答案。
 */
public record InterviewQuestionItem(
        int order,
        long subjectId,
        int difficulty,
        String content) {
}

package com.yunshu.zhixue.modules.interview.dto;

/**
 * Redis 面试会话中的题目快照，含参考答案，不返回给前端。
 */
public record InterviewTempQuestion(
        int order,
        long questionId,
        String content,
        String referenceAnswer,
        int difficulty) {
}

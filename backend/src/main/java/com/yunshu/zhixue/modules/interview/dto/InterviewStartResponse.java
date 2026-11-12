package com.yunshu.zhixue.modules.interview.dto;

import java.util.List;

/**
 * P-I02 发起面试响应。
 */
public record InterviewStartResponse(
        String sessionId,
        List<InterviewQuestionItem> questions,
        int questionCount) {
}

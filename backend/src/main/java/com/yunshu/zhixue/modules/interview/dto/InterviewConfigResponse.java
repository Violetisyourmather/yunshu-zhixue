package com.yunshu.zhixue.modules.interview.dto;

import java.util.List;

/**
 * P-I01 面试配置。
 */
public record InterviewConfigResponse(
        List<InterviewSubjectOption> subjects,
        List<Integer> difficulties,
        List<Integer> questionCountOptions,
        int defaultCount,
        int remainingCount,
        int timeoutMinutes) {
}

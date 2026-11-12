package com.yunshu.zhixue.modules.interview.dto;

import com.yunshu.zhixue.modules.ai.dto.AiPoint;

import java.util.List;

/**
 * Redis 面试会话中暂存的单题评分。
 */
public record InterviewTempGrade(
        int order,
        double score,
        String feedback,
        boolean degraded,
        List<AiPoint> referencePoints) {
}

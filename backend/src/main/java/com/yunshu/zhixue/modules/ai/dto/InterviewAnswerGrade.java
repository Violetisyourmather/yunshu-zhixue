package com.yunshu.zhixue.modules.ai.dto;

import java.util.List;

/**
 * AI 面试单题评分。
 */
public record InterviewAnswerGrade(
        double score,
        String feedback,
        List<AiPoint> referencePoints,
        boolean degraded) {
}

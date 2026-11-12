package com.yunshu.zhixue.modules.interview.dto;

import com.yunshu.zhixue.modules.ai.dto.InterviewImproveCard;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * P-I05 面试报告详情。
 */
public record InterviewReportResponse(
        long reportId,
        long recordId,
        long subjectId,
        String subjectName,
        int difficulty,
        int questionCount,
        double overallScore,
        Double trendDelta,
        Map<String, Double> dimensions,
        List<String> strengths,
        List<String> weaknesses,
        List<String> suggestions,
        List<InterviewImproveCard> improveCards,
        List<InterviewQuestionReview> questionReviews,
        int totalDuration,
        LocalDateTime completedAt) {
}

package com.yunshu.zhixue.modules.interview.dto;

import java.time.LocalDateTime;

/**
 * P-I07 面试记录列表项。
 */
public record InterviewRecordItem(
        long recordId,
        Long reportId,
        long subjectId,
        String subjectName,
        int difficulty,
        int questionCount,
        Double overallScore,
        Integer totalDuration,
        LocalDateTime completedAt,
        boolean hasReport) {
}

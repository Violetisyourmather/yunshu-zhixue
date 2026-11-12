package com.yunshu.zhixue.modules.student.dto;

import java.time.LocalDateTime;

/**
 * A-U07 学员 AI 面试记录。
 */
public record StudentInterviewItem(
        long id,
        long subjectId,
        String subjectName,
        int difficulty,
        int questionCount,
        Double overallScore,
        Integer totalDuration,
        LocalDateTime startedAt,
        LocalDateTime completedAt) {
}

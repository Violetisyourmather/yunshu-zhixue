package com.yunshu.zhixue.modules.question.dto;

import java.time.LocalDateTime;

/**
 * A-Q02 题目列表项。
 */
public record QuestionItem(
        long id,
        String content,
        long stageId,
        String stageName,
        long subjectId,
        String subjectName,
        int type,
        int difficulty,
        int status,
        String rejectReason,
        String tags,
        LocalDateTime createdAt,
        long doneCount,
        long correctCount,
        double accuracyRate) {
}

package com.yunshu.zhixue.modules.question.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A-Q03 题目详情（含选项与答题统计）。
 */
public record QuestionDetail(
        long id,
        long stageId,
        String stageName,
        long subjectId,
        String subjectName,
        int type,
        int difficulty,
        String content,
        String answer,
        String analysis,
        String tags,
        int status,
        String rejectReason,
        LocalDateTime createdAt,
        List<QuestionOptionItem> options,
        long doneCount,
        long correctCount,
        double accuracyRate) {
}

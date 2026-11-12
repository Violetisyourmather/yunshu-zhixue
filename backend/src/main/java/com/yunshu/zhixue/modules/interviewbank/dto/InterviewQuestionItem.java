package com.yunshu.zhixue.modules.interviewbank.dto;

import java.time.LocalDateTime;

/**
 * A-I01 面试题库列表项。
 */
public record InterviewQuestionItem(
        long id,
        String content,
        long subjectId,
        String subjectName,
        String topic,
        int difficulty,
        String tags,
        int sourceType,
        String sourceDocName,
        Integer sourceSeq,
        int status,
        LocalDateTime createdAt) {
}

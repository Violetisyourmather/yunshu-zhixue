package com.yunshu.zhixue.modules.interviewbank.dto;

import java.time.LocalDateTime;

/**
 * A-I02 面试题详情。
 */
public record InterviewQuestionDetail(
        long id,
        String content,
        String referenceAnswer,
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

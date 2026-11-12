package com.yunshu.zhixue.modules.interviewbank.dto;

/**
 * A-I10 主题词表项。
 */
public record InterviewTopicItem(
        long id,
        String name,
        Long subjectId,
        String subjectName,
        long questionCount,
        int sortOrder) {
}

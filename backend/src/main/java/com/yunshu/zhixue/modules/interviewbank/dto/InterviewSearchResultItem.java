package com.yunshu.zhixue.modules.interviewbank.dto;

/**
 * A-I09 检索测试命中项：题目内容 + 来源，用于验证解析/导入效果。
 */
public record InterviewSearchResultItem(
        long id,
        String content,
        String referenceAnswer,
        long subjectId,
        String subjectName,
        String topic,
        int difficulty,
        String sourceDocName,
        Integer sourceSeq) {
}

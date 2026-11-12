package com.yunshu.zhixue.modules.interviewbank.dto;

import java.util.List;

/**
 * 面试题 Excel 导入的逐行解析结果。
 *
 * @param row       Excel 行号（表头为第 1 行）
 * @param newTopic  主题不在词表中，确认导入时将自动创建
 */
public record InterviewImportRow(
        int sheetNo,
        String sheetName,
        int row,
        String subjectName,
        Long subjectId,
        String content,
        Integer difficulty,
        String topic,
        boolean newTopic,
        String tags,
        String referenceAnswer,
        String status,
        List<InterviewImportIssue> errors,
        List<InterviewImportIssue> warnings) {
}

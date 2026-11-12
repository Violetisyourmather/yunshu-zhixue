package com.yunshu.zhixue.modules.interviewbank.dto;

import java.util.List;

/**
 * 面试题导入预览。
 */
public record InterviewImportPreview(
        String token,
        String fileName,
        int totalRows,
        int validCount,
        int warningCount,
        int invalidCount,
        List<String> newTopics,
        List<InterviewImportRow> rows) {
}

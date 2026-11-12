package com.yunshu.zhixue.modules.question.dto;

import java.util.List;

/**
 * A-Q12 导入预览。
 *
 * @param token         预览令牌（Redis 暂存 30 分钟）；无可导入行时为 null
 * @param newStageNames 确认导入时将自动创建的阶段名
 */
public record QuestionImportPreview(
        String token,
        long subjectId,
        String subjectName,
        int totalRows,
        int validCount,
        int warningCount,
        int invalidCount,
        List<String> newStageNames,
        List<QuestionImportRow> rows) {
}

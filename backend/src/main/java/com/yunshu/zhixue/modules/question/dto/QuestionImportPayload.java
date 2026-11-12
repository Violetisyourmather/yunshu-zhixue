package com.yunshu.zhixue.modules.question.dto;

import java.util.List;

/**
 * 导入预览暂存在 Redis 中的完整载荷。
 */
public record QuestionImportPayload(long subjectId, List<QuestionImportRow> rows) {
}

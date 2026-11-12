package com.yunshu.zhixue.modules.course.dto;

import java.util.List;

/**
 * A-C06 导入预览。
 *
 * @param token 预览令牌（Redis 暂存 30 分钟）；无任何可导入行时为 null
 */
public record CourseImportPreview(
        String token,
        int totalRows,
        int validCount,
        int warningCount,
        int invalidCount,
        List<CourseImportRow> rows) {
}

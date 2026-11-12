package com.yunshu.zhixue.modules.course.dto;

import java.util.List;

/**
 * A-C06 课程 Excel 导入的逐行解析结果。
 *
 * <p>{@code status}：{@code valid} 有效 / {@code warning} 警告（仍可导入）/ {@code invalid} 无效（跳过）。</p>
 *
 * @param subjectId  主学科（null = 其他学科）
 * @param subjectIds 额外学科（不含主学科）
 * @param subjectNames 文件中「所属学科」原文，便于管理员核对
 */
public record CourseImportRow(
        int row,
        String name,
        Integer durationMinutes,
        String subjectNames,
        Long subjectId,
        List<Long> subjectIds,
        Integer source,
        String bvid,
        String coverUrl,
        String link,
        String status,
        List<CourseImportIssue> errors,
        List<CourseImportIssue> warnings) {
}

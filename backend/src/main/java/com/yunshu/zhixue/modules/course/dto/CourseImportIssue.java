package com.yunshu.zhixue.modules.course.dto;

/**
 * 导入行级问题（字段 + 可读原因）。
 */
public record CourseImportIssue(String field, String reason) {
}

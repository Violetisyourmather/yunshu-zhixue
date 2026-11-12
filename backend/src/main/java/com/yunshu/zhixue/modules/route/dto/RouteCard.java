package com.yunshu.zhixue.modules.route.dto;

import java.util.List;

/**
 * A-R01 学习路线卡片。
 *
 * @param courseCount    有效课程数
 * @param totalMinutes   课程总时长（分钟）
 * @param previewCourses 前 4 门课程名称，用于卡片课程预览
 */
public record RouteCard(
        long id,
        String name,
        String icon,
        long subjectId,
        String subjectName,
        int status,
        String targetEducation,
        String description,
        long courseCount,
        long totalMinutes,
        List<RouteCoursePreview> previewCourses) {
}

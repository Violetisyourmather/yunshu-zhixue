package com.yunshu.zhixue.modules.route.dto;

/**
 * P-R01 学员可见的学习路线。
 *
 * @param courseCount          路线上架课程数
 * @param completedCourseCount 当前学员已完成课程数
 * @param progress             完成进度百分比（0~100，保留 1 位小数）
 */
public record PortalRouteCard(
        long id,
        String name,
        long subjectId,
        String subjectName,
        String targetEducation,
        String description,
        long courseCount,
        long completedCourseCount,
        double progress) {
}

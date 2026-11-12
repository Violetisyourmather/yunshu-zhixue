package com.yunshu.zhixue.modules.route.dto;

import java.util.List;

/**
 * A-R06 学习路线详情 / 预览。
 */
public record RouteDetail(
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
        List<RouteCourseItem> courses) {
}

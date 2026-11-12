package com.yunshu.zhixue.modules.route.dto;

import java.util.List;

/**
 * P-R02 学员学习路线详情。
 */
public record PortalRouteDetail(
        long id,
        String name,
        long subjectId,
        String subjectName,
        String targetEducation,
        String description,
        List<PortalRouteCourseItem> courses) {
}

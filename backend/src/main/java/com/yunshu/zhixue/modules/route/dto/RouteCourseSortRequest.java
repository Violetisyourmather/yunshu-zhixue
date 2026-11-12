package com.yunshu.zhixue.modules.route.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * A-R09 路线课程排序。
 *
 * @param orderedIds 按新顺序排列的 route_course.id（必须是当前路线全部课程的一个排列）
 */
public record RouteCourseSortRequest(
        @NotEmpty(message = "课程顺序不能为空")
        List<Long> orderedIds) {
}

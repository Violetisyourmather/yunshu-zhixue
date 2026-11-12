package com.yunshu.zhixue.modules.route.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * P-R03 标记课程完成入参。
 */
public record RouteCourseCompleteRequest(
        @NotNull(message = "课程 id 不能为空")
        @Positive(message = "课程 id 不合法")
        Long courseId) {
}

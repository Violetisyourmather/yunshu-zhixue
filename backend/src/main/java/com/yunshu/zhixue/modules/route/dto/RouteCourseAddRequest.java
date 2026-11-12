package com.yunshu.zhixue.modules.route.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * A-R07 从课程库添加课程（支持多选）。
 */
public record RouteCourseAddRequest(
        @NotEmpty(message = "请选择要添加的课程")
        List<Long> courseIds) {
}

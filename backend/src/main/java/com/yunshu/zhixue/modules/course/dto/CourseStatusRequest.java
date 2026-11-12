package com.yunshu.zhixue.modules.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * A-C08 上架 / 下架。
 */
public record CourseStatusRequest(
        @NotNull(message = "请选择目标状态")
        @Min(value = 1, message = "课程状态不合法")
        @Max(value = 2, message = "课程状态不合法")
        Integer status) {
}

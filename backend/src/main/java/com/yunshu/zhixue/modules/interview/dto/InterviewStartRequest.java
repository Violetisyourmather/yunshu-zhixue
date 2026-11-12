package com.yunshu.zhixue.modules.interview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * P-I02 发起面试入参。
 */
public record InterviewStartRequest(
        @NotNull(message = "请选择面试方向")
        Long subjectId,

        @NotNull(message = "请选择面试难度")
        @Min(value = 1, message = "面试难度不合法")
        @Max(value = 3, message = "面试难度不合法")
        Integer difficulty,

        @NotNull(message = "请选择题目数量")
        Integer count) {
}

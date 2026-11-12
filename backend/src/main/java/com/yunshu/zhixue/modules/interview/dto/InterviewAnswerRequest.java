package com.yunshu.zhixue.modules.interview.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * P-I03 / P-I04 单题作答。
 */
public record InterviewAnswerRequest(
        @NotNull(message = "题号不能为空")
        @Min(value = 1, message = "题号不合法")
        Integer order,

        String studentAnswer,

        @Min(value = 0, message = "答题用时不能为负数")
        Integer durationSeconds) {
}

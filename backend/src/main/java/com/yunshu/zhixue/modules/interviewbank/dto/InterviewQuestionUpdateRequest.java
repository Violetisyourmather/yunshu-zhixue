package com.yunshu.zhixue.modules.interviewbank.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * A-I04 编辑面试题：仅主题 + 难度。
 */
public record InterviewQuestionUpdateRequest(
        @Size(max = 50, message = "主题不能超过 50 个字符")
        String topic,

        @Min(value = 1, message = "难度不合法")
        @Max(value = 3, message = "难度不合法")
        Integer difficulty) {
}

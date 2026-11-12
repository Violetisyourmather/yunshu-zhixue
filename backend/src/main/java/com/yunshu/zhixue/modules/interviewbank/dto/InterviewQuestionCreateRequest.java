package com.yunshu.zhixue.modules.interviewbank.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A-I03 手工录入面试题。
 */
public record InterviewQuestionCreateRequest(
        @NotNull(message = "请选择所属方向")
        Long subjectId,

        @NotBlank(message = "题干不能为空")
        @Size(max = 10000, message = "题干不能超过 10000 个字符")
        String content,

        @Size(max = 10000, message = "参考答案不能超过 10000 个字符")
        String referenceAnswer,

        @Size(max = 50, message = "主题不能超过 50 个字符")
        String topic,

        @Min(value = 1, message = "难度不合法")
        @Max(value = 3, message = "难度不合法")
        Integer difficulty,

        @Size(max = 255, message = "标签不能超过 255 个字符")
        String tags) {
}

package com.yunshu.zhixue.modules.interviewbank.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 确认导入 Markdown 面试题：整批统一指定方向、主题、难度与标签。
 */
public record InterviewMarkdownConfirmRequest(
        @NotBlank(message = "导入令牌不能为空")
        String token,

        @NotNull(message = "请选择所属方向")
        Long subjectId,

        @Min(value = 1, message = "难度不合法")
        @Max(value = 3, message = "难度不合法")
        Integer difficulty,

        @Size(max = 50, message = "主题不能超过 50 个字符")
        String topic,

        @Size(max = 255, message = "标签不能超过 255 个字符")
        String tags) {
}

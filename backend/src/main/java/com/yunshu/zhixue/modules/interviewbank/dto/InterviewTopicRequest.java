package com.yunshu.zhixue.modules.interviewbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A-I11 / A-I12 新增或编辑主题。
 */
public record InterviewTopicRequest(
        @NotBlank(message = "主题名称不能为空")
        @Size(max = 50, message = "主题名称不能超过 50 个字符")
        String name,

        Long subjectId) {
}

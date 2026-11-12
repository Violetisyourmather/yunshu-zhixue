package com.yunshu.zhixue.modules.interviewbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A-I09 面试题库检索测试入参。
 */
public record InterviewSearchTestRequest(
        @NotBlank(message = "检索关键词不能为空")
        @Size(max = 100, message = "关键词不能超过 100 个字符")
        String keyword) {
}

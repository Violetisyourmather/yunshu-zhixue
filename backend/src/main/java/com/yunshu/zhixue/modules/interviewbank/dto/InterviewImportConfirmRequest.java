package com.yunshu.zhixue.modules.interviewbank.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 确认导入面试题。
 */
public record InterviewImportConfirmRequest(
        @NotBlank(message = "导入令牌不能为空")
        String token) {
}

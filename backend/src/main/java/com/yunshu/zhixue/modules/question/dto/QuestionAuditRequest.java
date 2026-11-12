package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A-Q07 题目审核。
 */
public record QuestionAuditRequest(
        @NotNull(message = "请选择审核结果")
        Boolean pass,

        @Size(max = 255, message = "驳回原因不能超过 255 个字符")
        String rejectReason) {
}

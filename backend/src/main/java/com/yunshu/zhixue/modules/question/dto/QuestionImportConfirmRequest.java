package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * A-Q13 确认导入。
 */
public record QuestionImportConfirmRequest(
        @NotBlank(message = "导入令牌不能为空")
        String token) {
}

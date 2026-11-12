package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * A-Q10 批量上架 / 下架。
 */
public record QuestionBatchStatusRequest(
        @NotEmpty(message = "请选择题目")
        List<Long> ids,

        @NotBlank(message = "请选择操作")
        @Pattern(regexp = "on|off", message = "操作仅支持 on / off")
        String action) {
}

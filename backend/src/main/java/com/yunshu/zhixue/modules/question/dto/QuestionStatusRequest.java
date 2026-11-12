package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * A-Q08 上架 / 下架。
 */
public record QuestionStatusRequest(
        @NotBlank(message = "请选择操作")
        @Pattern(regexp = "on|off", message = "操作仅支持 on / off")
        String action) {
}

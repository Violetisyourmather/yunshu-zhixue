package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * A-M04 批量启用 / 禁用。
 */
public record BatchStatusRequest(
        @NotEmpty(message = "请选择用户")
        List<Long> ids,

        @NotNull(message = "请选择目标状态")
        @Min(value = 0, message = "状态值不合法")
        @Max(value = 1, message = "状态值不合法")
        Integer status) {
}

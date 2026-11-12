package com.yunshu.zhixue.modules.route.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * A-R04 启用 / 停用学习路线。
 */
public record RouteStatusRequest(
        @NotNull(message = "请选择目标状态")
        @Min(value = 0, message = "路线状态不合法")
        @Max(value = 1, message = "路线状态不合法")
        Integer status) {
}

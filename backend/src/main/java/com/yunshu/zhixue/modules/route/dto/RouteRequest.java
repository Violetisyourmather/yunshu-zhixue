package com.yunshu.zhixue.modules.route.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A-R02 / A-R03 新建或编辑学习路线基本信息。
 */
public record RouteRequest(
        @NotBlank(message = "路线名称不能为空")
        @Size(max = 100, message = "路线名称不能超过 100 个字符")
        String name,

        @NotNull(message = "请选择关联学科")
        Long subjectId,

        @Min(value = 0, message = "路线状态不合法")
        @Max(value = 1, message = "路线状态不合法")
        Integer status,

        @Size(max = 50, message = "路线图标不能超过 50 个字符")
        String icon,

        @Size(max = 100, message = "适合学历不能超过 100 个字符")
        String targetEducation,

        @Size(max = 1000, message = "路线描述不能超过 1000 个字符")
        String description) {
}

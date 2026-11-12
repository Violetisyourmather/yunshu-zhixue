package com.yunshu.zhixue.modules.subject.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A-S07 / A-S08 新增或编辑阶段（需求 4.3.1 阶段表单）。
 *
 * <p>图标可留空，留空时服务端存 NULL，前端展示默认图标。</p>
 */
public record StageRequest(
        @NotBlank(message = "阶段名称不能为空")
        @Size(max = 50, message = "阶段名称不能超过 50 个字符")
        String name,

        @Size(max = 50, message = "阶段图标不能超过 50 个字符")
        String icon,

        @NotNull(message = "请输入排序号")
        @Min(value = 0, message = "排序号不能小于 0")
        @Max(value = 9999, message = "排序号不能大于 9999")
        Integer sortOrder) {
}

package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A-L02 新增角色。
 */
public record RoleCreateRequest(
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 50, message = "角色名称不能超过 50 个字符")
        String name,

        @NotBlank(message = "角色编码不能为空")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{1,49}$",
                message = "角色编码需以字母开头，仅含字母、数字与下划线，长度 2-50")
        String code,

        @Size(max = 255, message = "角色描述不能超过 255 个字符")
        String description) {
}

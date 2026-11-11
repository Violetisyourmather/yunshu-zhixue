package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A-M03 编辑后台用户。
 *
 * <p>{@code password} 留空表示不修改密码，非空时由服务层校验 6-20 位。
 * 手机号在 DTO 层允许留空（内置超级管理员部署时可能未配置手机号，
 * 见 03 §4.5），常规账号的必填校验由服务层完成。</p>
 */
public record UserUpdateRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 50, message = "用户名不能超过 50 个字符")
        String username,

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String mobile,

        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱不能超过 100 个字符")
        String email,

        @NotNull(message = "请选择角色")
        Long roleId,

        String password,

        Integer status) {
}

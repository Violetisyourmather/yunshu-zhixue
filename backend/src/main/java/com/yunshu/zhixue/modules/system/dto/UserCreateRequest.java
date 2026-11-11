package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A-M02 新增后台用户。
 *
 * <p>角色不允许为超级管理员（需求 4.5.1），服务层二次校验。</p>
 */
public record UserCreateRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 50, message = "用户名不能超过 50 个字符")
        String username,

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String mobile,

        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱不能超过 100 个字符")
        String email,

        @NotNull(message = "请选择角色")
        Long roleId,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
        String password,

        Integer status) {
}

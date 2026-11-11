package com.yunshu.zhixue.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A-A01 后台登录入参（Q4）：{@code account} 为用户名或手机号。
 */
public record AdminLoginRequest(
        @NotBlank(message = "请输入用户名或手机号")
        @Size(max = 50, message = "用户名或手机号长度不能超过 50 个字符")
        String account,

        @NotBlank(message = "请输入密码")
        @Size(min = 6, max = 20, message = "密码长度为 6~20 位")
        String password) {
}

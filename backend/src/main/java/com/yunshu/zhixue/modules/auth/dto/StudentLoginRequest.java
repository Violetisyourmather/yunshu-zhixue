package com.yunshu.zhixue.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * P-A03 密码登录入参：{@code account} 为手机号或邮箱（不支持昵称登录）。
 */
public record StudentLoginRequest(
        @NotBlank(message = "请输入手机号或邮箱")
        @Size(max = 100, message = "手机号或邮箱长度不能超过 100 个字符")
        String account,

        @NotBlank(message = "请输入密码")
        @Size(min = 6, max = 20, message = "密码长度为 6~20 位")
        String password) {
}

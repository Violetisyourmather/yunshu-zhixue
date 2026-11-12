package com.yunshu.zhixue.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * P-A01 发送短信验证码入参。
 *
 * <p>{@code type} 仅允许 register / login / reset，对应注册、验证码登录、找回密码。</p>
 */
public record SmsCodeRequest(
        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String mobile,

        @NotBlank(message = "验证码类型不能为空")
        @Pattern(regexp = "register|login|reset", message = "验证码类型不合法")
        String type) {
}

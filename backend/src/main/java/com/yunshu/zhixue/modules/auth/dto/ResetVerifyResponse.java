package com.yunshu.zhixue.modules.auth.dto;

/**
 * P-A05 找回密码验证通过后返回的一次性重置令牌（TTL 10 分钟）。
 */
public record ResetVerifyResponse(String resetToken) {
}

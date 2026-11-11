package com.yunshu.zhixue.modules.auth.dto;

/**
 * 管理端双令牌响应（A-A01 / A-A02）：access 用于接口鉴权，refresh 仅用于续期。
 */
public record AdminLoginResponse(
        String accessToken,
        String refreshToken,
        AdminProfile admin) {
}

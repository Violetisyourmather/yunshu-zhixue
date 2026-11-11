package com.yunshu.zhixue.modules.auth.dto;

/**
 * 学员端双令牌响应（P-A03 / P-A07）：access 用于接口鉴权，refresh 仅用于续期。
 *
 * <p>与学员端 {@code LoginResult} 类型一致；个人信息由 P-M01 单独获取。</p>
 */
public record StudentLoginResponse(
        String accessToken,
        String refreshToken) {
}

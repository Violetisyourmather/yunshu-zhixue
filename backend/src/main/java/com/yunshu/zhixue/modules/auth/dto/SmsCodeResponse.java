package com.yunshu.zhixue.modules.auth.dto;

/**
 * P-A01 发送短信验证码响应。
 *
 * @param expireSeconds 验证码有效期（秒）
 * @param devCode       开发环境 mock 短信时回显的验证码；生产环境为 null
 */
public record SmsCodeResponse(int expireSeconds, String devCode) {
}

package com.yunshu.zhixue.modules.auth;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.auth.dto.ResetPasswordRequest;
import com.yunshu.zhixue.modules.auth.dto.ResetVerifyRequest;
import com.yunshu.zhixue.modules.auth.dto.ResetVerifyResponse;
import com.yunshu.zhixue.modules.auth.dto.SmsCodeRequest;
import com.yunshu.zhixue.modules.auth.dto.SmsCodeResponse;
import com.yunshu.zhixue.modules.auth.dto.SmsLoginRequest;
import com.yunshu.zhixue.modules.auth.dto.StudentLoginRequest;
import com.yunshu.zhixue.modules.auth.dto.StudentLoginResponse;
import com.yunshu.zhixue.modules.auth.dto.StudentRegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学员端认证接口（04 §4.1 P-A03 / P-A07 / P-A08）。
 *
 * <p>{@code /auth/**} 在 {@code SecurityConfig} 中整体放行：登录、刷新天然公开，
 * 登出按 P-A08「幂等」要求也允许重复调用（服务端只拉黑请求自带的令牌）。</p>
 */
@Tag(name = "学员认证", description = "P-A01 ~ P-A08 注册、登录、找回密码、刷新与登出")
@RestController
@RequestMapping("/api/portal/v1/auth")
public class PortalAuthController {

    private final PortalAuthService authService;
    private final SmsCodeService smsCodeService;

    public PortalAuthController(PortalAuthService authService, SmsCodeService smsCodeService) {
        this.authService = authService;
        this.smsCodeService = smsCodeService;
    }

    @Operation(summary = "P-A01 发送短信验证码（公开）")
    @PostMapping("/sms-code")
    public ApiResponse<SmsCodeResponse> sendSmsCode(@Valid @RequestBody SmsCodeRequest request) {
        return ApiResponse.ok(smsCodeService.send(request));
    }

    @Operation(summary = "P-A02 注册（公开，成功后直接登录）")
    @PostMapping("/register")
    public ApiResponse<StudentLoginResponse> register(@Valid @RequestBody StudentRegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @Operation(summary = "P-A03 密码登录（公开，手机号或邮箱）")
    @PostMapping("/login/password")
    public ApiResponse<StudentLoginResponse> login(@Valid @RequestBody StudentLoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "P-A04 手机验证码登录（公开）")
    @PostMapping("/login/sms")
    public ApiResponse<StudentLoginResponse> loginBySms(@Valid @RequestBody SmsLoginRequest request) {
        return ApiResponse.ok(authService.loginBySms(request));
    }

    @Operation(summary = "P-A05 找回密码·验证验证码（公开）")
    @PostMapping("/reset/verify")
    public ApiResponse<ResetVerifyResponse> verifyResetCode(@Valid @RequestBody ResetVerifyRequest request) {
        return ApiResponse.ok(authService.verifyResetCode(request));
    }

    @Operation(summary = "P-A06 找回密码·重置新密码（公开）")
    @PostMapping("/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "P-A07 刷新令牌（携带 X-Refresh-Token）")
    @PostMapping("/refresh")
    public ApiResponse<StudentLoginResponse> refresh(
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        return ApiResponse.ok(authService.refresh(refreshToken));
    }

    @Operation(summary = "P-A08 登出（幂等）")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        authService.logout(bearerToken(authorization), refreshToken);
        return ApiResponse.ok();
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}

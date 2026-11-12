package com.yunshu.zhixue.modules.auth;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.auth.dto.AdminLoginRequest;
import com.yunshu.zhixue.modules.auth.dto.AdminLoginResponse;
import com.yunshu.zhixue.modules.auth.dto.StudentLoginResponse;
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
 * 管理端认证接口（04 §5.1 A-A01 ~ A-A03）。
 *
 * <p>{@code /login}、{@code /refresh} 在 {@code SecurityConfig} 中放行；
 * {@code /logout} 需携带有效 accessToken（Authorization: Bearer）。</p>
 */
@Tag(name = "后台认证", description = "A-A01 登录 / A-A02 刷新 / A-A03 登出")
@RestController
@RequestMapping("/api/admin/v1/auth")
public class AdminAuthController {

    private final AdminAuthService authService;
    private final PortalPreviewService portalPreviewService;

    public AdminAuthController(AdminAuthService authService,
                               PortalPreviewService portalPreviewService) {
        this.authService = authService;
        this.portalPreviewService = portalPreviewService;
    }

    @Operation(summary = "A-A01 后台登录（公开）")
    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "A-A02 刷新令牌（公开，携带 X-Refresh-Token）")
    @PostMapping("/refresh")
    public ApiResponse<AdminLoginResponse> refresh(
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        return ApiResponse.ok(authService.refresh(refreshToken));
    }

    @Operation(summary = "A-A03 登出（幂等）")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        authService.logout(bearerToken(authorization), refreshToken);
        return ApiResponse.ok();
    }

    @Operation(summary = "学员端内测令牌（管理端已登录，免二次登录进入学员端）")
    @PostMapping("/portal-preview-token")
    public ApiResponse<StudentLoginResponse> portalPreviewToken() {
        return ApiResponse.ok(portalPreviewService.issueTokens());
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}

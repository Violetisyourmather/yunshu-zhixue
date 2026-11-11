package com.yunshu.zhixue.modules.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.constant.RedisKeys;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.auth.dto.AdminLoginRequest;
import com.yunshu.zhixue.modules.auth.dto.AdminLoginResponse;
import com.yunshu.zhixue.modules.auth.dto.AdminProfile;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.entity.Role;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.mapper.PermissionMapper;
import com.yunshu.zhixue.modules.system.mapper.RoleMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogRecorder;
import com.yunshu.zhixue.security.Audience;
import com.yunshu.zhixue.security.JwtPayload;
import com.yunshu.zhixue.security.JwtTokenProvider;
import com.yunshu.zhixue.security.TokenBlacklist;
import com.yunshu.zhixue.security.TokenType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 管理端认证（A-A01 ~ A-A03）：
 *
 * <ul>
 *   <li>登录：用户名优先、手机号兜底；BCrypt 校验；连续 5 次失败锁定 5 分钟（D18）。</li>
 *   <li>刷新：校验管理端 refreshToken，轮换旧令牌并重新加载角色权限。</li>
 *   <li>登出：access / refresh 的 jti 写入黑名单，幂等。</li>
 * </ul>
 *
 * <p>Redis 故障时失败计数降级为「跳过锁定」，不阻断登录（与 TokenBlacklist 的降级策略一致）。</p>
 */
@Service
public class AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);

    private final AdminUserMapper adminUserMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final StringRedisTemplate redis;
    private final OperationLogRecorder operationLogRecorder;

    public AdminAuthService(AdminUserMapper adminUserMapper,
                            RoleMapper roleMapper,
                            PermissionMapper permissionMapper,
                            PasswordEncoder passwordEncoder,
                            JwtTokenProvider tokenProvider,
                            TokenBlacklist tokenBlacklist,
                            StringRedisTemplate redis,
                            OperationLogRecorder operationLogRecorder) {
        this.adminUserMapper = adminUserMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.tokenBlacklist = tokenBlacklist;
        this.redis = redis;
        this.operationLogRecorder = operationLogRecorder;
    }

    /**
     * A-A01 登录。
     *
     * @throws BizException 30009 账号或密码错误 / 30010 临时锁定 / 30011 账号已禁用
     */
    public AdminLoginResponse login(AdminLoginRequest request) {
        String account = request.account().trim();
        String failKey = RedisKeys.adminLoginFail(account);

        if (isLocked(failKey)) {
            throw new BizException(ErrorCode.ADMIN_ACCOUNT_LOCKED);
        }

        AdminUser admin = findByAccount(account);
        if (admin == null || !passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            recordFailure(failKey);
            throw new BizException(ErrorCode.ADMIN_BAD_CREDENTIALS);
        }
        if (!admin.enabled()) {
            throw new BizException(ErrorCode.ADMIN_DISABLED);
        }

        clearFailures(failKey);
        touchLastLogin(admin.getId());
        operationLogRecorder.recordFor(admin.getId(), admin.getUsername(),
                OperationActions.LOGIN, "登录认证", "登录系统");
        return issueTokens(admin);
    }

    /**
     * A-A02 刷新令牌：旧 refreshToken 轮换后立即失效；账号被禁用或删除则要求重新登录。
     */
    public AdminLoginResponse refresh(String refreshToken) {
        JwtPayload payload = parseRefreshToken(refreshToken);
        AdminUser admin = adminUserMapper.selectById(payload.userId());
        if (admin == null || !admin.enabled()) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        revokeQuietly(payload);
        return issueTokens(admin);
    }

    /**
     * A-A03 登出：accessToken 与 refreshToken 均拉黑；无效令牌视为已登出（幂等）。
     */
    public void logout(String accessToken, String refreshToken) {
        revokeQuietly(accessToken, TokenType.ACCESS);
        revokeQuietly(refreshToken, TokenType.REFRESH);
        operationLogRecorder.record(OperationActions.LOGOUT, "登录认证", "退出登录");
    }

    private AdminUser findByAccount(String account) {
        AdminUser admin = adminUserMapper.selectOne(
                Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getUsername, account));
        if (admin == null) {
            admin = adminUserMapper.selectOne(
                    Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getMobile, account));
        }
        return admin;
    }

    private AdminLoginResponse issueTokens(AdminUser admin) {
        Role role = roleMapper.selectById(admin.getRoleId());
        if (role == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "账号角色配置异常，请联系系统管理员");
        }
        List<String> permissions = permissionMapper.selectCodesByRoleId(admin.getRoleId());
        Set<String> permissionSet = new LinkedHashSet<>(permissions);

        String accessToken = tokenProvider.createAccessToken(Audience.ADMIN, admin.getId(), permissionSet);
        String refreshToken = tokenProvider.createRefreshToken(Audience.ADMIN, admin.getId());
        AdminProfile profile = new AdminProfile(
                admin.getId(), admin.getUsername(), role.getCode(), role.getName(), permissions);
        return new AdminLoginResponse(accessToken, refreshToken, profile);
    }

    private void touchLastLogin(Long adminId) {
        AdminUser update = new AdminUser();
        update.setId(adminId);
        update.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
        adminUserMapper.updateById(update);
    }

    private JwtPayload parseRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        JwtPayload payload = tokenProvider.parse(refreshToken, Audience.ADMIN, TokenType.REFRESH);
        if (tokenBlacklist.isRevoked(payload.jti())) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return payload;
    }

    private void revokeQuietly(String token, TokenType type) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            revokeQuietly(tokenProvider.parse(token, Audience.ADMIN, type));
        } catch (BizException ex) {
            // 令牌本身无效或已过期：登出幂等，视为已失效
        }
    }

    private void revokeQuietly(JwtPayload payload) {
        try {
            tokenBlacklist.revoke(payload.jti(), payload.expiresAt());
        } catch (DataAccessException ex) {
            log.warn("令牌黑名单写入失败，登出降级放行：{}", ex.getMessage());
        }
    }

    private boolean isLocked(String failKey) {
        try {
            String failures = redis.opsForValue().get(failKey);
            return failures != null && Integer.parseInt(failures) >= AppConstants.LOGIN_MAX_FAILURES;
        } catch (DataAccessException | NumberFormatException ex) {
            log.warn("登录失败计数读取失败，跳过锁定校验：{}", ex.getMessage());
            return false;
        }
    }

    private void recordFailure(String failKey) {
        try {
            Long failures = redis.opsForValue().increment(failKey);
            if (failures == null) {
                return;
            }
            Duration lockTtl = Duration.ofMinutes(AppConstants.LOGIN_LOCK_MINUTES);
            if (failures == 1) {
                redis.expire(failKey, lockTtl);
            }
            if (failures >= AppConstants.LOGIN_MAX_FAILURES) {
                redis.expire(failKey, lockTtl);
                throw new BizException(ErrorCode.ADMIN_ACCOUNT_LOCKED);
            }
        } catch (DataAccessException ex) {
            log.warn("登录失败计数写入失败，跳过锁定：{}", ex.getMessage());
        }
    }

    private void clearFailures(String failKey) {
        try {
            redis.delete(failKey);
        } catch (DataAccessException ex) {
            log.warn("登录失败计数清理失败：{}", ex.getMessage());
        }
    }
}

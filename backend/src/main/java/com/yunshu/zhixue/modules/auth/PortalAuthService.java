package com.yunshu.zhixue.modules.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.constant.RedisKeys;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.auth.dto.StudentLoginRequest;
import com.yunshu.zhixue.modules.auth.dto.StudentLoginResponse;
import com.yunshu.zhixue.modules.student.entity.Student;
import com.yunshu.zhixue.modules.student.mapper.StudentMapper;
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
import java.util.List;
import java.util.Set;

/**
 * 学员端认证（P-A03 / P-A07 / P-A08）：
 *
 * <ul>
 *   <li>登录：手机号或邮箱匹配，BCrypt 校验；连续 5 次失败锁定 5 分钟（D18）。</li>
 *   <li>刷新：校验学员端 refreshToken，轮换旧令牌；账号禁用或删除则要求重新登录。</li>
 *   <li>登出：access / refresh 的 jti 写入黑名单，幂等。</li>
 * </ul>
 *
 * <p>Redis 故障时失败计数降级为「跳过锁定」，不阻断登录（与 TokenBlacklist 的降级策略一致）。</p>
 */
@Service
public class PortalAuthService {

    private static final Logger log = LoggerFactory.getLogger(PortalAuthService.class);

    private final StudentMapper studentMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final StringRedisTemplate redis;

    public PortalAuthService(StudentMapper studentMapper,
                             PasswordEncoder passwordEncoder,
                             JwtTokenProvider tokenProvider,
                             TokenBlacklist tokenBlacklist,
                             StringRedisTemplate redis) {
        this.studentMapper = studentMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.tokenBlacklist = tokenBlacklist;
        this.redis = redis;
    }

    /**
     * P-A03 密码登录。
     *
     * @throws BizException 20003 账号或密码错误 / 20004 临时锁定 / 20005 账号已禁用
     */
    public StudentLoginResponse login(StudentLoginRequest request) {
        String account = request.account().trim();
        String failKey = RedisKeys.portalLoginFail(account);

        if (isLocked(failKey)) {
            throw new BizException(ErrorCode.ACCOUNT_LOCKED);
        }

        Student student = findByAccount(account);
        if (student == null || !passwordEncoder.matches(request.password(), student.getPasswordHash())) {
            recordFailure(failKey);
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (!student.enabled()) {
            throw new BizException(ErrorCode.STUDENT_DISABLED);
        }

        clearFailures(failKey);
        touchLastLogin(student.getId());
        return issueTokens(student);
    }

    /**
     * P-A07 刷新令牌：旧 refreshToken 轮换后立即失效；账号被禁用或删除则要求重新登录。
     */
    public StudentLoginResponse refresh(String refreshToken) {
        JwtPayload payload = parseRefreshToken(refreshToken);
        Student student = studentMapper.selectById(payload.userId());
        if (student == null || !student.enabled()) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        revokeQuietly(payload);
        return issueTokens(student);
    }

    /**
     * P-A08 登出：accessToken 与 refreshToken 均拉黑；无效令牌视为已登出（幂等）。
     */
    public void logout(String accessToken, String refreshToken) {
        revokeQuietly(accessToken, TokenType.ACCESS);
        revokeQuietly(refreshToken, TokenType.REFRESH);
    }

    /**
     * 手机号优先、邮箱兜底。邮箱虽然由唯一索引保障，仍按「命中多条即失败」防御，
     * 避免大小写/排序规则差异导致的账号枚举（P-A03）。
     */
    private Student findByAccount(String account) {
        Student student = studentMapper.selectOne(
                Wrappers.<Student>lambdaQuery().eq(Student::getMobile, account));
        if (student != null) {
            return student;
        }
        List<Student> matched = studentMapper.selectList(
                Wrappers.<Student>lambdaQuery().eq(Student::getEmail, account));
        return matched.size() == 1 ? matched.get(0) : null;
    }

    private StudentLoginResponse issueTokens(Student student) {
        String accessToken = tokenProvider.createAccessToken(Audience.PORTAL, student.getId(), Set.of());
        String refreshToken = tokenProvider.createRefreshToken(Audience.PORTAL, student.getId());
        return new StudentLoginResponse(accessToken, refreshToken);
    }

    private void touchLastLogin(Long studentId) {
        Student update = new Student();
        update.setId(studentId);
        update.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
        studentMapper.updateById(update);
    }

    private JwtPayload parseRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        JwtPayload payload = tokenProvider.parse(refreshToken, Audience.PORTAL, TokenType.REFRESH);
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
            revokeQuietly(tokenProvider.parse(token, Audience.PORTAL, type));
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
                throw new BizException(ErrorCode.ACCOUNT_LOCKED);
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

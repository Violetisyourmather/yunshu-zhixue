package com.yunshu.zhixue.modules.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.constant.RedisKeys;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.auth.dto.SmsCodeRequest;
import com.yunshu.zhixue.modules.auth.dto.SmsCodeResponse;
import com.yunshu.zhixue.modules.student.entity.Student;
import com.yunshu.zhixue.modules.student.mapper.StudentMapper;
import com.yunshu.zhixue.security.RequestRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 学员端短信验证码（P-A01）。
 *
 * <p>验证码 6 位数字、TTL 5 分钟；同一手机号 60 秒冷却、每日最多 10 次。
 * 开发环境通过 {@code app.sms.mock=true} 使用日志/响应回显代替真实短信发送，
 * 生产环境必须接入短信服务商后再关闭 mock。</p>
 */
@Service
public class SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> TYPES = Set.of("register", "login", "reset");
    private static final int CODE_BOUND = 1_000_000;

    private final StringRedisTemplate redis;
    private final StudentMapper studentMapper;
    private final RequestRateLimiter rateLimiter;
    private final boolean mock;

    public SmsCodeService(StringRedisTemplate redis,
                          StudentMapper studentMapper,
                          RequestRateLimiter rateLimiter,
                          @Value("${app.sms.mock:false}") boolean mock) {
        this.redis = redis;
        this.studentMapper = studentMapper;
        this.rateLimiter = rateLimiter;
        this.mock = mock;
    }

    /** P-A01 发送验证码。 */
    public SmsCodeResponse send(SmsCodeRequest request) {
        String type = request.type();
        String mobile = request.mobile();
        requireType(type);
        rateLimiter.check("sms", AppConstants.RATE_LIMIT_SMS, "acct:" + mobile);

        boolean registered = isRegistered(mobile);
        if ("register".equals(type) && registered) {
            throw new BizException(ErrorCode.CONTACT_OCCUPIED, "该手机号已注册，请直接登录");
        }
        if ("reset".equals(type) && !registered) {
            throw new BizException(ErrorCode.NOT_FOUND, "该手机号尚未注册");
        }
        if (!mock) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "短信服务暂未配置，请联系管理员");
        }

        try {
            String cooldownKey = RedisKeys.SMS_COOLDOWN + mobile;
            if (Boolean.TRUE.equals(redis.hasKey(cooldownKey))) {
                throw new BizException(ErrorCode.RATE_LIMITED, "验证码发送过于频繁，请稍后再试");
            }

            String dayKey = RedisKeys.SMS_DAY_COUNT + mobile;
            long todayCount = parseCount(redis.opsForValue().get(dayKey));
            if (todayCount >= AppConstants.SMS_DAILY_LIMIT) {
                throw new BizException(ErrorCode.SMS_DAILY_LIMIT);
            }

            String code = randomCode();
            redis.opsForValue().set(RedisKeys.smsCode(type, mobile), code,
                    Duration.ofMinutes(AppConstants.SMS_CODE_TTL_MINUTES));
            redis.opsForValue().set(cooldownKey, "1",
                    Duration.ofSeconds(AppConstants.SMS_RESEND_COOLDOWN_SECONDS));
            Long dayCount = redis.opsForValue().increment(dayKey);
            if (dayCount != null && dayCount == 1L) {
                redis.expire(dayKey, ttlUntilMidnight());
            }

            log.info("【开发环境短信】手机号 {} 的 {} 验证码：{}", mobile, type, code);
            int expireSeconds = AppConstants.SMS_CODE_TTL_MINUTES * 60;
            return new SmsCodeResponse(expireSeconds, code);
        } catch (DataAccessException ex) {
            log.error("短信验证码写入失败：mobile={}, type={}", mobile, type, ex);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "验证码服务暂不可用");
        }
    }

    /**
     * 校验验证码，成功后立即失效。
     *
     * @throws BizException 20002 验证码错误或已失效
     */
    public void verify(String mobile, String type, String code) {
        requireType(type);
        try {
            String key = RedisKeys.smsCode(type, mobile);
            String stored = redis.opsForValue().get(key);
            if (stored == null) {
                throw new BizException(ErrorCode.SMS_CODE_INVALID);
            }
            if (!stored.equals(code)) {
                recordVerifyFailure(mobile, type, key);
                throw new BizException(ErrorCode.SMS_CODE_INVALID);
            }
            redis.delete(key);
            redis.delete(verifyFailKey(mobile, type));
        } catch (DataAccessException ex) {
            log.error("短信验证码校验失败：mobile={}, type={}", mobile, type, ex);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "验证码服务暂不可用");
        }
    }

    private void recordVerifyFailure(String mobile, String type, String codeKey) {
        String failKey = verifyFailKey(mobile, type);
        Long failures = redis.opsForValue().increment(failKey);
        if (failures != null && failures == 1L) {
            redis.expire(failKey, Duration.ofMinutes(AppConstants.SMS_CODE_TTL_MINUTES));
        }
        if (failures != null && failures >= AppConstants.VERIFY_CODE_MAX_FAILURES) {
            redis.delete(codeKey);
            redis.delete(failKey);
        }
    }

    private boolean isRegistered(String mobile) {
        return studentMapper.selectCount(
                Wrappers.<Student>lambdaQuery().eq(Student::getMobile, mobile)) > 0;
    }

    private String randomCode() {
        return String.format("%06d", RANDOM.nextInt(CODE_BOUND));
    }

    private static long parseCount(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String verifyFailKey(String mobile, String type) {
        return RedisKeys.SMS_VERIFY_FAIL + type + ":" + mobile;
    }

    private static Duration ttlUntilMidnight() {
        LocalDateTime now = LocalDateTime.now(AppConstants.BUSINESS_ZONE);
        LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, nextMidnight);
    }

    private static void requireType(String type) {
        if (!TYPES.contains(type)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "验证码类型不合法");
        }
    }
}

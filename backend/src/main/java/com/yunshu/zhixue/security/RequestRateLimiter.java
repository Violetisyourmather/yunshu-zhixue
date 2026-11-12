package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.constant.RedisKeys;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 敏感接口频控（需求 3.2 / 4.1，D18）。
 *
 * <p>按「客户端 IP + 账号」双维度固定窗口计数，任一维度超限即返回 10005（429）。
 * 阈值由 {@link AppConstants} 固定，不向前端暴露；Redis 故障时降级放行，不阻断业务。</p>
 */
@Service
public class RequestRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RequestRateLimiter.class);

    private final StringRedisTemplate redis;

    public RequestRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 检查并累计请求次数。
     *
     * @param scope             接口标识，如 login / register / interview-start
     * @param limit             窗口内最大次数
     * @param accountIdentities 账号维度标识，如 {@code acct:13800138000}；调用方自行加前缀区分维度
     */
    public void check(String scope, int limit, String... accountIdentities) {
        List<String> keys = new ArrayList<>();
        String ip = clientIp();
        if (StringUtils.hasText(ip)) {
            keys.add(RedisKeys.RATE_LIMIT + scope + ":ip:" + ip);
        }
        if (accountIdentities != null) {
            for (String identity : accountIdentities) {
                if (StringUtils.hasText(identity)) {
                    keys.add(RedisKeys.RATE_LIMIT + scope + ":" + identity.trim());
                }
            }
        }
        for (String key : keys) {
            if (increment(key) > limit) {
                throw new BizException(ErrorCode.RATE_LIMITED);
            }
        }
    }

    private long increment(String key) {
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, Duration.ofSeconds(AppConstants.RATE_LIMIT_WINDOW_SECONDS));
            }
            return count == null ? 0L : count;
        } catch (DataAccessException ex) {
            log.warn("接口频控计数失败，降级放行：key={}, error={}", key, ex.getMessage());
            return 0L;
        }
    }

    /** 客户端 IP：兼容 Nginx 反向代理头。 */
    private String clientIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}

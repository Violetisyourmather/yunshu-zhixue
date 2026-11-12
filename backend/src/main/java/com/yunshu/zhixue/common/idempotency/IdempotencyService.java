package com.yunshu.zhixue.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.constant.RedisKeys;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 幂等键支持（04 §2.13）。
 *
 * <p>可选请求头 {@code Idempotency-Key}：首次请求正常执行并缓存结果（TTL 24h），
 * 相同键的重复请求直接返回首次结果；并发重复请求返回 10004「请求正在处理中」。
 * 未携带请求头时行为不变；Redis 故障时降级为直接执行，不阻断业务。</p>
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final Duration TTL = Duration.ofHours(24);
    private static final String PENDING = "PENDING";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public IdempotencyService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行带幂等保护的写操作。
     *
     * @param scope          接口标识，避免不同接口使用相同键时互相命中
     * @param idempotencyKey 客户端请求头 Idempotency-Key（可为空）
     * @param resultType     返回类型（用于反序列化首次结果）
     * @param action         实际业务逻辑
     */
    public <T> T execute(String scope, String idempotencyKey, Class<T> resultType,
                         Supplier<T> action) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return action.get();
        }
        String key = buildKey(scope, idempotencyKey);

        String cached = readQuietly(key);
        if (cached != null) {
            return resolve(cached, resultType);
        }
        Boolean acquired = acquireQuietly(key);
        if (!Boolean.TRUE.equals(acquired)) {
            cached = readQuietly(key);
            if (cached != null) {
                return resolve(cached, resultType);
            }
            throw new BizException(ErrorCode.CONFLICT, "请求正在处理中，请勿重复提交");
        }

        try {
            T result = action.get();
            cacheQuietly(key, result);
            return result;
        } catch (RuntimeException ex) {
            deleteQuietly(key);
            throw ex;
        }
    }

    private String buildKey(String scope, String idempotencyKey) {
        Long userId = SecurityUtils.currentUserIdOrNull();
        String owner = userId == null ? "anon" : String.valueOf(userId);
        return RedisKeys.IDEMPOTENCY + owner + ":" + scope + ":" + idempotencyKey.trim();
    }

    private <T> T resolve(String cached, Class<T> resultType) {
        if (PENDING.equals(cached)) {
            throw new BizException(ErrorCode.CONFLICT, "请求正在处理中，请勿重复提交");
        }
        try {
            return objectMapper.readValue(cached, resultType);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.CONFLICT, "重复请求的结果解析失败，请稍后重试");
        }
    }

    private String readQuietly(String key) {
        try {
            return redis.opsForValue().get(key);
        } catch (DataAccessException ex) {
            log.warn("幂等键读取失败，降级执行：{}", ex.getMessage());
            return null;
        }
    }

    private Boolean acquireQuietly(String key) {
        try {
            return redis.opsForValue().setIfAbsent(key, PENDING, TTL);
        } catch (DataAccessException ex) {
            log.warn("幂等键占用失败，降级执行：{}", ex.getMessage());
            return true;
        }
    }

    private void cacheQuietly(String key, Object result) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(result), TTL);
        } catch (Exception ex) {
            log.warn("幂等结果缓存失败：{}", ex.getMessage());
        }
    }

    private void deleteQuietly(String key) {
        try {
            redis.delete(key);
        } catch (DataAccessException ex) {
            log.warn("幂等键释放失败：{}", ex.getMessage());
        }
    }
}

package com.yunshu.zhixue.modules.interview.service;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.constant.RedisKeys;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AI 面试每日配额（D18）：每日 5 次，按 Asia/Shanghai 日界重置。
 */
@Service
public class InterviewQuotaService {

    private static final Logger log = LoggerFactory.getLogger(InterviewQuotaService.class);
    private static final DateTimeFormatter DATE_KEY = DateTimeFormatter.BASIC_ISO_DATE;

    private final StringRedisTemplate redis;

    public InterviewQuotaService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 今日剩余次数；Redis 不可用时返回 0，禁用发起。 */
    public int remaining(long studentId) {
        String key = key(studentId);
        try {
            String value = redis.opsForValue().get(key);
            long used = value == null ? 0 : Long.parseLong(value);
            return (int) Math.max(0, AppConstants.INTERVIEW_DAILY_QUOTA - used);
        } catch (DataAccessException | NumberFormatException ex) {
            log.warn("面试配额读取失败，剩余次数按 0 处理：{}", ex.getMessage());
            return 0;
        }
    }

    /** 扣减一次配额；超出上限时回滚并返回 20007。 */
    public void consume(long studentId) {
        String key = key(studentId);
        try {
            Long used = redis.opsForValue().increment(key);
            if (used == null) {
                throw new BizException(ErrorCode.INTERNAL_ERROR, "面试服务暂不可用");
            }
            if (used > AppConstants.INTERVIEW_DAILY_QUOTA) {
                redis.opsForValue().decrement(key);
                throw new BizException(ErrorCode.INTERVIEW_QUOTA_EXHAUSTED);
            }
            if (used == 1L) {
                redis.expire(key, ttlUntilMidnight());
            }
        } catch (DataAccessException ex) {
            log.error("面试配额扣减失败：studentId={}", studentId, ex);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "面试服务暂不可用");
        }
    }

    private static String key(long studentId) {
        return RedisKeys.interviewQuota(studentId,
                LocalDate.now(AppConstants.BUSINESS_ZONE).format(DATE_KEY));
    }

    private static Duration ttlUntilMidnight() {
        LocalDateTime now = LocalDateTime.now(AppConstants.BUSINESS_ZONE);
        LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, nextMidnight);
    }
}

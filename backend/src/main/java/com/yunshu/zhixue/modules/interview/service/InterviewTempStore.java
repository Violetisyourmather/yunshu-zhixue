package com.yunshu.zhixue.modules.interview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.constant.RedisKeys;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.interview.dto.InterviewTempSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 面试会话 Redis 暂存（D4-4 / D16）。
 *
 * <p>发起面试不落库；题目快照与逐题评分暂存在 Redis，结束后一次性写入数据库。</p>
 */
@Service
public class InterviewTempStore {

    private static final Logger log = LoggerFactory.getLogger(InterviewTempStore.class);
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public InterviewTempStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void save(InterviewTempSession session) {
        try {
            redis.opsForValue().set(key(session.getSessionId()),
                    objectMapper.writeValueAsString(session), TTL);
        } catch (Exception ex) {
            log.error("面试会话暂存失败：{}", session.getSessionId(), ex);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "面试会话保存失败，请稍后重试");
        }
    }

    public InterviewTempSession load(String sessionId) {
        try {
            String raw = redis.opsForValue().get(key(sessionId));
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return objectMapper.readValue(raw, InterviewTempSession.class);
        } catch (DataAccessException ex) {
            log.warn("面试会话读取失败：{}", sessionId, ex);
            return null;
        } catch (Exception ex) {
            log.warn("面试会话解析失败：{}", sessionId, ex);
            return null;
        }
    }

    public void delete(String sessionId) {
        try {
            redis.delete(key(sessionId));
        } catch (DataAccessException ex) {
            log.warn("面试会话删除失败：{}", sessionId, ex);
        }
    }

    private static String key(String sessionId) {
        return RedisKeys.INTERVIEW_TEMP + sessionId;
    }
}

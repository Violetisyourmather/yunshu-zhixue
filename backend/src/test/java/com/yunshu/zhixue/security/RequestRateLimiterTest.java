package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestRateLimiterTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RequestRateLimiter limiter;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        limiter = new RequestRateLimiter(redis);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void blocksWhenIpOrAccountExceedsLimit() {
        when(valueOperations.increment(anyString())).thenReturn(1L, 1L, 2L, 2L, 3L, 3L);

        limiter.check("login", 2, "acct:13800138000");
        limiter.check("login", 2, "acct:13800138000");

        assertThatThrownBy(() -> limiter.check("login", 2, "acct:13800138000"))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RATE_LIMITED);
        verify(valueOperations, times(3)).increment("rate:login:ip:10.0.0.8");
        // 第三次在 IP 维度即被拦截，账号维度只累计了前两次
        verify(valueOperations, times(2)).increment("rate:login:acct:13800138000");
    }

    @Test
    void failsOpenWhenRedisUnavailable() {
        when(valueOperations.increment(anyString()))
                .thenThrow(new RedisConnectionFailureException("redis down"));

        assertThatCode(() -> limiter.check("login", 2, "acct:13800138000"))
                .doesNotThrowAnyException();
    }
}

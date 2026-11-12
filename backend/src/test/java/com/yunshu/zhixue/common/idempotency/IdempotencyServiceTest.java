package com.yunshu.zhixue.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IdempotencyServiceTest {

    private static final String KEY = "idempotency:anon:test-scope:key-1";

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        service = new IdempotencyService(redis, objectMapper);
    }

    @Test
    void executesDirectlyWhenKeyMissing() {
        String result = service.execute("test-scope", null, String.class, () -> "value");

        assertThat(result).isEqualTo("value");
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    void returnsCachedResultWithoutExecuting() throws Exception {
        when(valueOperations.get(KEY)).thenReturn(objectMapper.writeValueAsString("cached"));
        AtomicInteger calls = new AtomicInteger();

        String result = service.execute("test-scope", "key-1", String.class, () -> {
            calls.incrementAndGet();
            return "fresh";
        });

        assertThat(result).isEqualTo("cached");
        assertThat(calls).hasValue(0);
    }

    @Test
    void rejectsConcurrentDuplicateWhilePending() {
        when(valueOperations.get(KEY)).thenReturn("PENDING");

        assertThatThrownBy(() -> service.execute("test-scope", "key-1", String.class, () -> "value"))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void cachesFirstSuccessfulResult() throws Exception {
        when(valueOperations.get(KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(eq(KEY), eq("PENDING"), any(Duration.class))).thenReturn(true);

        String result = service.execute("test-scope", "key-1", String.class, () -> "fresh");

        assertThat(result).isEqualTo("fresh");
        verify(valueOperations).set(eq(KEY), eq(objectMapper.writeValueAsString("fresh")),
                any(Duration.class));
    }

    @Test
    void releasesKeyWhenActionFails() {
        when(valueOperations.get(KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(eq(KEY), eq("PENDING"), any(Duration.class))).thenReturn(true);

        assertThatThrownBy(() -> service.execute("test-scope", "key-1", String.class, () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        verify(redis).delete(KEY);
    }

    @Test
    void returnsFirstResultWhenConcurrentRequestFinishesFirst() throws Exception {
        when(valueOperations.get(KEY)).thenReturn(null)
                .thenReturn(objectMapper.writeValueAsString("winner"));
        when(valueOperations.setIfAbsent(eq(KEY), eq("PENDING"), any(Duration.class))).thenReturn(false);

        String result = service.execute("test-scope", "key-1", String.class, () -> "loser");

        assertThat(result).isEqualTo("winner");
    }
}

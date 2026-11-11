package com.yunshu.zhixue.common.exception;

import com.yunshu.zhixue.common.enums.Difficulty;
import com.yunshu.zhixue.common.enums.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorCodeTest {

    @Test
    void codesAreUniqueFiveDigits() {
        Set<Integer> seen = new HashSet<>();
        for (ErrorCode errorCode : ErrorCode.values()) {
            assertTrue(errorCode.code() >= 10000 && errorCode.code() <= 39999,
                    "业务码应为 5 位：" + errorCode);
            assertTrue(seen.add(errorCode.code()), "业务码重复：" + errorCode.code());
            assertFalse(errorCode.message().isBlank(), "错误信息不能为空：" + errorCode);
            assertNotNull(errorCode.httpStatus());
        }
    }

    @Test
    void httpStatusIsFailureStatus() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            assertTrue(errorCode.httpStatus().isError(),
                    "业务错误必须对应 4xx/5xx：" + errorCode);
        }
    }

    @Test
    void enumResolvesByDbCode() {
        assertEquals(QuestionType.SHORT_ANSWER, QuestionType.fromCode(4));
        assertEquals(Difficulty.MEDIUM, Difficulty.fromCode(2));
        assertThrows(BizException.class, () -> QuestionType.fromCode(9));
        assertThrows(BizException.class, () -> Difficulty.fromCode(null));
    }
}

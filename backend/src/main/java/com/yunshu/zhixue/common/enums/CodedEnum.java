package com.yunshu.zhixue.common.enums;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;

/**
 * 数值枚举公共契约（04 附录 C：API 取值与 DB 存储值完全一致，不做字符串别名转换）。
 */
public interface CodedEnum {

    int code();

    String label();

    static <E extends Enum<E> & CodedEnum> E fromCode(Class<E> type, Integer code) {
        if (code == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "枚举值不能为空");
        }
        for (E value : type.getEnumConstants()) {
            if (value.code() == code) {
                return value;
            }
        }
        throw new BizException(ErrorCode.PARAM_INVALID, "非法的枚举值：" + code);
    }
}

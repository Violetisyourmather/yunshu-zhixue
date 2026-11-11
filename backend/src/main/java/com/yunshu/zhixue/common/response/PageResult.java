package com.yunshu.zhixue.common.response;

import java.util.List;

/**
 * 统一分页响应（04 §2.4）：{@code {list, total, page, pageSize}}，页码从 1 起。
 */
public record PageResult<T>(List<T> list, long total, long page, long pageSize) {

    public static <T> PageResult<T> of(List<T> list, long total, long page, long pageSize) {
        return new PageResult<>(list, total, page, pageSize);
    }

    public static <T> PageResult<T> empty(long page, long pageSize) {
        return new PageResult<>(List.of(), 0, page, pageSize);
    }
}

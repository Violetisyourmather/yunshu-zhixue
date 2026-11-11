package com.yunshu.zhixue.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 统一响应体（04 §2.3 方案 A）。
 *
 * <p>成功：HTTP 2xx + {@code {code:0, message:"ok", data}}；
 * 失败：真实 HTTP 状态码 + {@code {code:业务码, message, data:null, fieldErrors?, fileErrors?}}。</p>
 *
 * <p>{@code fieldErrors} 仅参数校验失败（10000）返回；
 * {@code fileErrors} 仅批量导入校验失败（30005）返回；其余情况字段不出现。</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        int code,
        String message,
        T data,
        List<FieldErrorItem> fieldErrors,
        List<FileErrorItem> fileErrors) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data, null, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "ok", null, null, null);
    }

    public static ApiResponse<Void> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, null, null);
    }

    public static ApiResponse<Void> fail(int code, String message, List<FieldErrorItem> fieldErrors) {
        return new ApiResponse<>(code, message, null, fieldErrors, null);
    }

    public static ApiResponse<Void> failImport(int code, String message, List<FileErrorItem> fileErrors) {
        return new ApiResponse<>(code, message, null, null, fileErrors);
    }

    /** 字段级校验错误：{@code {"field":"mobile","reason":"手机号格式不正确"}}。 */
    public record FieldErrorItem(String field, String reason) {
    }

    /** 导入行级错误：{@code {"row":12,"field":"正确答案","reason":"..."}}。 */
    public record FileErrorItem(int row, String field, String reason) {
    }
}

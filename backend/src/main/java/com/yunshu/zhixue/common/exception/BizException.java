package com.yunshu.zhixue.common.exception;

/**
 * 业务异常：由全局异常处理器映射为「真实 HTTP 状态码 + 业务错误码」。
 *
 * <p>业务代码只抛该异常，不直接构造 HTTP 响应；错误信息面向用户可读，
 * 不得包含堆栈、SQL 或内部主机信息（api-rules §5）。</p>
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

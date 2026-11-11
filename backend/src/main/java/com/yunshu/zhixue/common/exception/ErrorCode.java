package com.yunshu.zhixue.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码（04 附录 B）。
 *
 * <p>5 位数字，首位标识归属：1 通用 / 2 学员端 / 3 管理端。HTTP 状态码与业务码一一对应，
 * 不出现「HTTP 200 + 业务失败」。</p>
 */
public enum ErrorCode {

    // ---------- 通用 1xxxx ----------
    PARAM_INVALID(10000, HttpStatus.BAD_REQUEST, "参数校验失败"),
    UNAUTHORIZED(10001, HttpStatus.UNAUTHORIZED, "未认证或凭证失效"),
    FORBIDDEN(10002, HttpStatus.FORBIDDEN, "无权限"),
    NOT_FOUND(10003, HttpStatus.NOT_FOUND, "资源不存在"),
    CONFLICT(10004, HttpStatus.CONFLICT, "资源冲突"),
    RATE_LIMITED(10005, HttpStatus.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试"),
    INTERNAL_ERROR(10006, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误"),
    BAD_REQUEST_BODY(10007, HttpStatus.BAD_REQUEST, "请求体格式错误"),
    FILE_INVALID(10008, HttpStatus.BAD_REQUEST, "文件类型或大小不符"),

    // ---------- 学员端 2xxxx ----------
    CONTACT_OCCUPIED(20001, HttpStatus.CONFLICT, "联系方式已被占用"),
    SMS_CODE_INVALID(20002, HttpStatus.BAD_REQUEST, "验证码错误或已失效"),
    BAD_CREDENTIALS(20003, HttpStatus.BAD_REQUEST, "账号或密码错误"),
    ACCOUNT_LOCKED(20004, HttpStatus.LOCKED, "账号已锁定，请稍后重试"),
    STUDENT_DISABLED(20005, HttpStatus.FORBIDDEN, "账号已禁用"),
    SMS_DAILY_LIMIT(20006, HttpStatus.TOO_MANY_REQUESTS, "今日验证码获取次数已达上限"),
    INTERVIEW_QUOTA_EXHAUSTED(20007, HttpStatus.TOO_MANY_REQUESTS, "今日 AI 面试次数已用完"),
    INTERVIEW_SESSION_NOT_FOUND(20008, HttpStatus.NOT_FOUND, "面试会话不存在或已过期"),
    INTERVIEW_QUESTION_INSUFFICIENT(20009, HttpStatus.CONFLICT, "该方向可出题数量不足"),
    AI_SERVICE_UNAVAILABLE(20010, HttpStatus.SERVICE_UNAVAILABLE, "AI 批改服务暂不可用"),
    WRONG_QUESTION_NOT_FOUND(20011, HttpStatus.NOT_FOUND, "错题不存在"),

    // ---------- 管理端 3xxxx ----------
    NAME_EXISTS(30001, HttpStatus.CONFLICT, "名称/用户名/编码已存在"),
    MOBILE_EXISTS(30002, HttpStatus.CONFLICT, "手机号已存在"),
    ROLE_IN_USE(30003, HttpStatus.CONFLICT, "角色下存在用户，不可删除"),
    BUILTIN_ACCOUNT_PROTECTED(30004, HttpStatus.CONFLICT, "内置账号或超级管理员角色不可操作"),
    IMPORT_INVALID(30005, HttpStatus.BAD_REQUEST, "导入校验失败"),
    STATUS_TRANSITION_DENIED(30006, HttpStatus.CONFLICT, "当前状态不允许该操作"),
    RELATED_CONTENT_EXISTS(30007, HttpStatus.CONFLICT, "存在关联内容，不可删除"),
    PERMISSION_SAVE_FAILED(30008, HttpStatus.BAD_REQUEST, "权限保存失败"),
    ADMIN_BAD_CREDENTIALS(30009, HttpStatus.BAD_REQUEST, "后台账号或密码错误"),
    ADMIN_ACCOUNT_LOCKED(30010, HttpStatus.LOCKED, "后台账号已临时锁定"),
    ADMIN_DISABLED(30011, HttpStatus.FORBIDDEN, "后台账号已禁用");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String message() {
        return message;
    }
}

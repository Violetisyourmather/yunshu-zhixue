package com.yunshu.zhixue.security;

/**
 * 令牌域（D7 / D12）：两端账号体系独立，token 互不通用。
 */
public enum Audience {

    PORTAL("portal"),
    ADMIN("admin");

    private final String value;

    Audience(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}

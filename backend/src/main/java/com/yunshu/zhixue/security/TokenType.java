package com.yunshu.zhixue.security;

/**
 * 令牌类型：access 用于接口鉴权，refresh 仅用于续期（不得直接访问业务接口）。
 */
public enum TokenType {

    ACCESS("access"),
    REFRESH("refresh");

    private final String value;

    TokenType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}

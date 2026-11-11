package com.yunshu.zhixue.modules.system.dto;

/**
 * A-M07 重置密码响应：明文密码仅在此响应中出现一次（04 §5.9）。
 */
public record ResetPasswordResponse(String password) {
}

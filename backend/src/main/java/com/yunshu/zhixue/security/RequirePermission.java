package com.yunshu.zhixue.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理端接口权限点声明（04 §2.8），取值与 {@code permission.code} 一致，
 * 如 {@code @RequirePermission("question:audit")}。
 *
 * <p>角色管理 5 个接口不设权限点，仅校验 {@code role.code = super_admin}。</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    String value();
}

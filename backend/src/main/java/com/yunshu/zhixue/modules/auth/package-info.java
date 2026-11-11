/**
 * 认证与账号模块（P-A01~P-A08、A-A01~A-A03）。
 *
 * <p>职责：注册、密码/验证码登录、找回密码、令牌刷新与登出；两端各自入口，
 * 令牌域由 {@code security} 保证互不通用。</p>
 *
 * <p>边界：不承载权限树与角色逻辑（归 {@code system}）；验证码与登录失败计数在 Redis。</p>
 *
 * <p>标准结构：{@code controller/portal}、{@code controller/admin}、{@code service}、
 * {@code mapper}、{@code entity}、{@code dto}、{@code convert}。</p>
 */
package com.yunshu.zhixue.modules.auth;

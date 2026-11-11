/**
 * 系统管理模块（A4：用户、角色、权限、操作日志、系统设置）。
 *
 * <p>数据表：{@code admin_user}、{@code role}、{@code permission}、
 * {@code role_permission}、{@code operation_log}、{@code system_setting}。</p>
 *
 * <p>职责：后台账号维护、角色与 9 组 35 项权限树、操作日志审计、平台基本信息。</p>
 *
 * <p>边界：超级管理员为内置唯一账号（{@code is_builtin=1}），不可删除、不可改角色；
 * 角色管理不设权限点，仅超级管理员可访问。</p>
 */
package com.yunshu.zhixue.modules.system;

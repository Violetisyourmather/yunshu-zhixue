package com.yunshu.zhixue.modules.system.dto;

import java.util.List;

/**
 * A-L01 角色卡片。
 *
 * @param builtin         内置超级管理员角色（不可编辑权限、不可删除）
 * @param permissionNames 形如「用户管理·查看」的权限标签；无权限时为空列表
 */
public record RoleCard(
        Long id,
        String name,
        String code,
        String description,
        long userCount,
        int permissionCount,
        List<String> permissionNames,
        boolean builtin) {
}

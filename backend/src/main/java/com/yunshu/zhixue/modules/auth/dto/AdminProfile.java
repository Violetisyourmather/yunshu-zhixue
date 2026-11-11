package com.yunshu.zhixue.modules.auth.dto;

import java.util.List;

/**
 * 登录响应的管理员信息与权限码（前端据此渲染菜单与按钮）。
 */
public record AdminProfile(
        long id,
        String username,
        String roleCode,
        String roleName,
        List<String> permissions) {
}

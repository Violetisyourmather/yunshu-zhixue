package com.yunshu.zhixue.modules.system.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.PermissionNode;
import com.yunshu.zhixue.modules.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限树（04 §5.10 A-L04，仅超级管理员）。
 */
@Tag(name = "权限树", description = "A-L04")
@RestController
@RequestMapping("/api/admin/v1/permissions")
public class PermissionController {

    private final RoleService roleService;

    public PermissionController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "A-L04 权限树（含角色勾选状态）")
    @GetMapping("/tree")
    public ApiResponse<List<PermissionNode>> tree(@RequestParam(required = false) Long roleId) {
        return ApiResponse.ok(roleService.permissionTree(roleId));
    }
}

package com.yunshu.zhixue.modules.system.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.RoleCard;
import com.yunshu.zhixue.modules.system.dto.RoleCreateRequest;
import com.yunshu.zhixue.modules.system.dto.RolePermissionSaveRequest;
import com.yunshu.zhixue.modules.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色权限（04 §5.10 A-L02/A-L03/A-L05）。
 *
 * <p>无权限点，仅超级管理员可访问；校验在 {@link RoleService} 内统一执行。</p>
 */
@Tag(name = "角色权限", description = "A-L01 ~ A-L05（仅超级管理员）")
@RestController
@RequestMapping("/api/admin/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "A-L01 角色卡片列表")
    @GetMapping
    public ApiResponse<List<RoleCard>> list() {
        return ApiResponse.ok(roleService.list());
    }

    @Operation(summary = "A-L02 新增角色")
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.ok(roleService.create(request));
    }

    @Operation(summary = "A-L03 删除角色")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-L05 保存角色权限（全量覆盖）")
    @PutMapping("/{id}/permissions")
    public ApiResponse<Void> savePermissions(@PathVariable Long id,
                                             @RequestBody(required = false) RolePermissionSaveRequest request) {
        roleService.savePermissions(id, request == null ? List.of() : request.permissionIds());
        return ApiResponse.ok();
    }
}

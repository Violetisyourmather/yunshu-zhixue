package com.yunshu.zhixue.modules.system.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.system.dto.BatchDeleteRequest;
import com.yunshu.zhixue.modules.system.dto.BatchStatusRequest;
import com.yunshu.zhixue.modules.system.dto.ResetPasswordResponse;
import com.yunshu.zhixue.modules.system.dto.RoleOption;
import com.yunshu.zhixue.modules.system.dto.UserCreateRequest;
import com.yunshu.zhixue.modules.system.dto.UserDetail;
import com.yunshu.zhixue.modules.system.dto.UserItem;
import com.yunshu.zhixue.modules.system.dto.UserUpdateRequest;
import com.yunshu.zhixue.modules.system.service.AdminUserService;
import com.yunshu.zhixue.security.RequirePermission;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理（04 §5.9 A-M01 ~ A-M08）。
 */
@Tag(name = "用户管理", description = "A-M01 ~ A-M08")
@RestController
@RequestMapping("/api/admin/v1/users")
public class AdminUserController {

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "A-M01 用户列表")
    @GetMapping
    @RequirePermission("user:view")
    public ApiResponse<PageResult<UserItem>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(userService.list(keyword, roleId, status, page, pageSize));
    }

    @Operation(summary = "用户管理页角色下拉")
    @GetMapping("/role-options")
    @RequirePermission("user:view")
    public ApiResponse<List<RoleOption>> roleOptions() {
        return ApiResponse.ok(userService.roleOptions());
    }

    @Operation(summary = "A-M02 新增用户")
    @PostMapping
    @RequirePermission("user:add")
    public ApiResponse<UserItem> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @Operation(summary = "A-M03 编辑用户")
    @PutMapping("/{id}")
    @RequirePermission("user:edit")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        userService.update(id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-M04 批量启用/禁用")
    @PutMapping("/batch-status")
    @RequirePermission("user:edit")
    public ApiResponse<Void> batchStatus(@Valid @RequestBody BatchStatusRequest request) {
        userService.batchStatus(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-M05 删除用户")
    @DeleteMapping("/{id}")
    @RequirePermission("user:disable")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-M06 批量删除")
    @DeleteMapping("/batch")
    @RequirePermission("user:disable")
    public ApiResponse<Void> batchDelete(@Valid @RequestBody BatchDeleteRequest request) {
        userService.batchDelete(request.ids());
        return ApiResponse.ok();
    }

    @Operation(summary = "A-M07 重置密码为部署期默认密码")
    @PostMapping("/{id}/reset-password")
    @RequirePermission("user:edit")
    public ApiResponse<ResetPasswordResponse> resetPassword(@PathVariable Long id) {
        return ApiResponse.ok(userService.resetPassword(id));
    }

    @Operation(summary = "A-M08 用户详情与最近操作")
    @GetMapping("/{id}/detail")
    @RequirePermission("user:view")
    public ApiResponse<UserDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(userService.detail(id));
    }
}

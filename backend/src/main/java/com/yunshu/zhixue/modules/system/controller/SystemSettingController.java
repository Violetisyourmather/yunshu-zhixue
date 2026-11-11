package com.yunshu.zhixue.modules.system.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.SystemSettingsRequest;
import com.yunshu.zhixue.modules.system.dto.SystemSettingsResponse;
import com.yunshu.zhixue.modules.system.service.SystemSettingService;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统设置（04 §5.12 A-T01 ~ A-T03）：读取登录即可，写入需 system:setting。
 */
@Tag(name = "系统设置", description = "A-T01 ~ A-T03")
@RestController
@RequestMapping("/api/admin/v1/settings")
public class SystemSettingController {

    private final SystemSettingService settingService;

    public SystemSettingController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    @Operation(summary = "A-T01 读取基本信息（登录即可）")
    @GetMapping
    public ApiResponse<SystemSettingsResponse> get() {
        return ApiResponse.ok(settingService.get());
    }

    @Operation(summary = "A-T02 保存基本信息")
    @PutMapping
    @RequirePermission("system:setting")
    public ApiResponse<SystemSettingsResponse> save(@Valid @RequestBody SystemSettingsRequest request) {
        return ApiResponse.ok(settingService.save(request));
    }

    @Operation(summary = "A-T03 恢复默认设置")
    @PostMapping("/reset")
    @RequirePermission("system:setting")
    public ApiResponse<SystemSettingsResponse> reset() {
        return ApiResponse.ok(settingService.reset());
    }
}

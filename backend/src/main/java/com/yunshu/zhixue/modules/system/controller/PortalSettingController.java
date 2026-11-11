package com.yunshu.zhixue.modules.system.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.SystemSettingsResponse;
import com.yunshu.zhixue.modules.system.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学员端平台信息（公开只读）：平台名称、描述、Logo 与默认头像。
 *
 * <p>学员端导航、登录页、浏览器标题与管理端登录页均需在未登录时渲染品牌，
 * 故该接口公开；写入仍走管理端 {@link SystemSettingController}。</p>
 */
@Tag(name = "平台信息", description = "P-T01 平台基本信息（公开）")
@RestController
@RequestMapping("/api/portal/v1/settings")
public class PortalSettingController {

    private final SystemSettingService settingService;

    public PortalSettingController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    @Operation(summary = "P-T01 读取平台基本信息（公开）")
    @GetMapping
    public ApiResponse<SystemSettingsResponse> get() {
        return ApiResponse.ok(settingService.get());
    }
}

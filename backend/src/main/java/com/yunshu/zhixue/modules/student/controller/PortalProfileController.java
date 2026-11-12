package com.yunshu.zhixue.modules.student.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.student.dto.ProfileUpdateRequest;
import com.yunshu.zhixue.modules.student.dto.StudentProfileResponse;
import com.yunshu.zhixue.modules.student.service.PortalProfileService;
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
 * 学员端个人中心接口（04 §4.8 P-M01 / P-M02 / P-M04）。
 */
@Tag(name = "学员个人中心", description = "P-M01 个人信息 / P-M02 修改资料 / P-M04 完成新手引导")
@RestController
@RequestMapping("/api/portal/v1/profile")
public class PortalProfileController {

    private final PortalProfileService profileService;

    public PortalProfileController(PortalProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "P-M01 读取个人信息")
    @GetMapping
    public ApiResponse<StudentProfileResponse> get() {
        return ApiResponse.ok(profileService.getProfile());
    }

    @Operation(summary = "P-M02 修改资料")
    @PutMapping
    public ApiResponse<Void> update(@Valid @RequestBody ProfileUpdateRequest request) {
        profileService.updateProfile(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "P-M04 完成新手引导（幂等）")
    @PostMapping("/guide")
    public ApiResponse<Void> completeGuide() {
        profileService.completeGuide();
        return ApiResponse.ok();
    }
}

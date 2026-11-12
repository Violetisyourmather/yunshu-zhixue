package com.yunshu.zhixue.modules.stats.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.stats.dto.DashboardOverview;
import com.yunshu.zhixue.modules.stats.dto.QuestionDistributionItem;
import com.yunshu.zhixue.modules.stats.dto.RecentActivityItem;
import com.yunshu.zhixue.modules.stats.dto.UserGrowthPoint;
import com.yunshu.zhixue.modules.stats.service.DashboardService;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端数据看板（04 §5.2 A-D01 ~ A-D04）。
 */
@Tag(name = "数据看板", description = "A-D01 概览 / A-D02 用户增长 / A-D03 题目分布 / A-D04 最近活动")
@RestController
@RequestMapping("/api/admin/v1/dashboard")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "A-D01 概览统计卡（缓存 5 分钟）")
    @GetMapping("/overview")
    @RequirePermission("dashboard:view")
    public ApiResponse<DashboardOverview> overview() {
        return ApiResponse.ok(dashboardService.overview());
    }

    @Operation(summary = "A-D02 用户增长曲线（7/30/90 天）")
    @GetMapping("/user-growth")
    @RequirePermission("dashboard:view")
    public ApiResponse<List<UserGrowthPoint>> userGrowth(
            @RequestParam(required = false) Integer range) {
        return ApiResponse.ok(dashboardService.userGrowth(range));
    }

    @Operation(summary = "A-D03 题目学科分布")
    @GetMapping("/question-distribution")
    @RequirePermission("dashboard:view")
    public ApiResponse<List<QuestionDistributionItem>> questionDistribution() {
        return ApiResponse.ok(dashboardService.questionDistribution());
    }

    @Operation(summary = "A-D04 最近活动（实时）")
    @GetMapping("/recent-activities")
    @RequirePermission("dashboard:view")
    public ApiResponse<PageResult<RecentActivityItem>> recentActivities(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(dashboardService.recentActivities(page, pageSize));
    }
}

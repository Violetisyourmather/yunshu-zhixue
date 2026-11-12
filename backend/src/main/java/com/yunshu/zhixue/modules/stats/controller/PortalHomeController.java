package com.yunshu.zhixue.modules.stats.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.stats.dto.HotSubjectItem;
import com.yunshu.zhixue.modules.stats.dto.LearningOverview;
import com.yunshu.zhixue.modules.stats.dto.PlatformStats;
import com.yunshu.zhixue.modules.stats.service.HomeStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学员端首页接口（04 §4.2 P-H01 ~ P-H03）。
 *
 * <p>平台数据与热门学科公开；学习概览需学员登录，未登录由 SecurityConfig 统一返回 401。</p>
 */
@Tag(name = "学员首页", description = "P-H01 平台数据 / P-H02 热门学科 / P-H03 学习概览")
@RestController
@RequestMapping("/api/portal/v1/home")
public class PortalHomeController {

    private final HomeStatsService homeStatsService;

    public PortalHomeController(HomeStatsService homeStatsService) {
        this.homeStatsService = homeStatsService;
    }

    @Operation(summary = "P-H01 平台数据（公开）")
    @GetMapping("/platform-stats")
    public ApiResponse<PlatformStats> platformStats() {
        return ApiResponse.ok(homeStatsService.platformStats());
    }

    @Operation(summary = "P-H02 热门学科（公开，按题目数倒序取 6 个）")
    @GetMapping("/hot-subjects")
    public ApiResponse<List<HotSubjectItem>> hotSubjects() {
        return ApiResponse.ok(homeStatsService.hotSubjects());
    }

    @Operation(summary = "P-H03 学习概览（登录）")
    @GetMapping("/learning-overview")
    public ApiResponse<LearningOverview> learningOverview() {
        return ApiResponse.ok(homeStatsService.learningOverview());
    }
}

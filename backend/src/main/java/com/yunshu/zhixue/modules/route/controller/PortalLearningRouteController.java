package com.yunshu.zhixue.modules.route.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.route.dto.PortalRouteCard;
import com.yunshu.zhixue.modules.route.dto.PortalRouteDetail;
import com.yunshu.zhixue.modules.route.dto.PortalRouteSubjectItem;
import com.yunshu.zhixue.modules.route.dto.RouteCourseCompleteRequest;
import com.yunshu.zhixue.modules.route.service.PortalLearningRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学员端学习路线（04 §4.6 P-R01 ~ P-R03）。
 */
@Tag(name = "学员学习路线", description = "P-R01 路线列表 / P-R02 路线详情 / P-R03 标记与取消完成")
@RestController
@RequestMapping("/api/portal/v1")
public class PortalLearningRouteController {

    private final PortalLearningRouteService routeService;

    public PortalLearningRouteController(PortalLearningRouteService routeService) {
        this.routeService = routeService;
    }

    @Operation(summary = "学习路线页学科选择卡")
    @GetMapping("/routes/subjects")
    public ApiResponse<List<PortalRouteSubjectItem>> subjects() {
        return ApiResponse.ok(routeService.subjects());
    }

    @Operation(summary = "P-R01 路线列表")
    @GetMapping("/routes")
    public ApiResponse<List<PortalRouteCard>> routes() {
        return ApiResponse.ok(routeService.routes());
    }

    @Operation(summary = "P-R02 路线详情")
    @GetMapping("/routes/{id}")
    public ApiResponse<PortalRouteDetail> detail(@PathVariable long id) {
        return ApiResponse.ok(routeService.detail(id));
    }

    @Operation(summary = "P-R03 标记课程完成（幂等）")
    @PostMapping("/route-courses/{id}/complete")
    public ApiResponse<Void> complete(@PathVariable long id,
                                      @Valid @RequestBody RouteCourseCompleteRequest request) {
        routeService.markComplete(id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "P-R03 取消课程完成（幂等）")
    @DeleteMapping("/route-courses/{id}/complete")
    public ApiResponse<Void> uncomplete(@PathVariable long id) {
        routeService.unmarkComplete(id);
        return ApiResponse.ok();
    }
}

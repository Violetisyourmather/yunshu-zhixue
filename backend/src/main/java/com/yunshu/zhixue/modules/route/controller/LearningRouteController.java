package com.yunshu.zhixue.modules.route.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.course.dto.SubjectRef;
import com.yunshu.zhixue.modules.route.dto.RouteCard;
import com.yunshu.zhixue.modules.route.dto.RouteCourseAddRequest;
import com.yunshu.zhixue.modules.route.dto.RouteCourseOption;
import com.yunshu.zhixue.modules.route.dto.RouteCourseSortRequest;
import com.yunshu.zhixue.modules.route.dto.RouteDetail;
import com.yunshu.zhixue.modules.route.dto.RouteRequest;
import com.yunshu.zhixue.modules.route.dto.RouteStatusRequest;
import com.yunshu.zhixue.modules.route.service.LearningRouteService;
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
 * 学习路线管理（04 §5.7 A-R01 ~ A-R09）。
 */
@Tag(name = "学习路线", description = "A-R01 ~ A-R09")
@RestController
@RequestMapping("/api/admin/v1/routes")
public class LearningRouteController {

    private final LearningRouteService routeService;

    public LearningRouteController(LearningRouteService routeService) {
        this.routeService = routeService;
    }

    @Operation(summary = "A-R01 学习路线卡片列表")
    @GetMapping
    @RequirePermission("route:view")
    public ApiResponse<List<RouteCard>> list() {
        return ApiResponse.ok(routeService.list());
    }

    @Operation(summary = "路线表单学科下拉")
    @GetMapping("/subject-options")
    @RequirePermission("route:view")
    public ApiResponse<List<SubjectRef>> subjectOptions() {
        return ApiResponse.ok(routeService.subjectOptions());
    }

    @Operation(summary = "A-R02 新建学习路线")
    @PostMapping
    @RequirePermission("route:add")
    public ApiResponse<Long> create(@Valid @RequestBody RouteRequest request) {
        return ApiResponse.ok(routeService.create(request));
    }

    @Operation(summary = "A-R03 编辑学习路线")
    @PutMapping("/{id}")
    @RequirePermission("route:edit")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody RouteRequest request) {
        routeService.update(id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-R04 启用 / 停用学习路线")
    @PostMapping("/{id}/status")
    @RequirePermission("route:edit")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody RouteStatusRequest request) {
        routeService.updateStatus(id, request.status());
        return ApiResponse.ok();
    }

    @Operation(summary = "A-R05 删除学习路线")
    @DeleteMapping("/{id}")
    @RequirePermission("route:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        routeService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-R06 学习路线详情 / 预览")
    @GetMapping("/{id}/detail")
    @RequirePermission("route:view")
    public ApiResponse<RouteDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(routeService.detail(id));
    }

    @Operation(summary = "A-R07 从课程库添加候选课程")
    @GetMapping("/{id}/course-options")
    @RequirePermission("route:view")
    public ApiResponse<List<RouteCourseOption>> courseOptions(
            @PathVariable Long id,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(routeService.courseOptions(id, keyword));
    }

    @Operation(summary = "A-R07 添加课程")
    @PostMapping("/{id}/courses")
    @RequirePermission("route:edit")
    public ApiResponse<Integer> addCourses(@PathVariable Long id,
                                           @Valid @RequestBody RouteCourseAddRequest request) {
        return ApiResponse.ok(routeService.addCourses(id, request.courseIds()));
    }

    @Operation(summary = "A-R08 移除课程")
    @DeleteMapping("/{id}/courses/{courseId}")
    @RequirePermission("route:edit")
    public ApiResponse<Void> removeCourse(@PathVariable Long id, @PathVariable Long courseId) {
        routeService.removeCourse(id, courseId);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-R09 路线课程排序")
    @PutMapping("/{id}/courses/sort")
    @RequirePermission("route:edit")
    public ApiResponse<Void> sortCourses(@PathVariable Long id,
                                         @Valid @RequestBody RouteCourseSortRequest request) {
        routeService.sortCourses(id, request.orderedIds());
        return ApiResponse.ok();
    }
}

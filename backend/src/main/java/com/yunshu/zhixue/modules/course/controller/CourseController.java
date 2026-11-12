package com.yunshu.zhixue.modules.course.controller;

import com.yunshu.zhixue.common.idempotency.IdempotencyService;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.course.dto.CourseImportConfirmRequest;
import com.yunshu.zhixue.modules.course.dto.CourseImportPreview;
import com.yunshu.zhixue.modules.course.dto.CourseImportResult;
import com.yunshu.zhixue.modules.course.dto.CourseImportTextRequest;
import com.yunshu.zhixue.modules.course.dto.CourseItem;
import com.yunshu.zhixue.modules.course.dto.CourseRequest;
import com.yunshu.zhixue.modules.course.dto.CourseStats;
import com.yunshu.zhixue.modules.course.dto.CourseStatusRequest;
import com.yunshu.zhixue.modules.course.dto.SubjectRef;
import com.yunshu.zhixue.modules.course.service.CourseImportService;
import com.yunshu.zhixue.modules.course.service.CourseService;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * B 站课程库（04 §5.4 A-C01 ~ A-C08）。
 */
@Tag(name = "课程库", description = "A-C01 ~ A-C08")
@RestController
@RequestMapping("/api/admin/v1/courses")
public class CourseController {

    private final CourseService courseService;
    private final CourseImportService courseImportService;
    private final IdempotencyService idempotencyService;

    public CourseController(CourseService courseService,
                            CourseImportService courseImportService,
                            IdempotencyService idempotencyService) {
        this.courseService = courseService;
        this.courseImportService = courseImportService;
        this.idempotencyService = idempotencyService;
    }

    @Operation(summary = "A-C01 课程统计卡")
    @GetMapping("/stats")
    @RequirePermission("course:view")
    public ApiResponse<CourseStats> stats() {
        return ApiResponse.ok(courseService.stats());
    }

    @Operation(summary = "A-C02 课程列表")
    @GetMapping
    @RequirePermission("course:view")
    public ApiResponse<PageResult<CourseItem>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer source,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(courseService.list(keyword, subjectId, source, status, page, pageSize));
    }

    @Operation(summary = "课程表单 / 筛选学科下拉")
    @GetMapping("/subject-options")
    @RequirePermission("course:view")
    public ApiResponse<List<SubjectRef>> subjectOptions() {
        return ApiResponse.ok(courseService.subjectOptions());
    }

    @Operation(summary = "A-C03 新增课程")
    @PostMapping
    @RequirePermission("course:add")
    public ApiResponse<Long> create(@Valid @RequestBody CourseRequest request) {
        return ApiResponse.ok(courseService.create(request));
    }

    @Operation(summary = "A-C04 编辑课程")
    @PutMapping("/{id}")
    @RequirePermission("course:edit")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        courseService.update(id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-C05 删除课程")
    @DeleteMapping("/{id}")
    @RequirePermission("course:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-C08 上架 / 下架")
    @PutMapping("/{id}/status")
    @RequirePermission("course:edit")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody CourseStatusRequest request) {
        courseService.updateStatus(id, request.status());
        return ApiResponse.ok();
    }

    @Operation(summary = "A-C06 批量导入预览（Excel）")
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("course:import")
    public ApiResponse<CourseImportPreview> preview(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(courseImportService.preview(file));
    }

    @Operation(summary = "A-C09 批量导入预览（粘贴文本）")
    @PostMapping("/import/preview-text")
    @RequirePermission("course:import")
    public ApiResponse<CourseImportPreview> previewText(
            @Valid @RequestBody CourseImportTextRequest request) {
        return ApiResponse.ok(courseImportService.previewText(request.text()));
    }

    @Operation(summary = "A-C07 确认批量导入")
    @PostMapping("/import/confirm")
    @RequirePermission("course:import")
    public ApiResponse<CourseImportResult> confirm(
            @Valid @RequestBody CourseImportConfirmRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        CourseImportResult result = idempotencyService.execute(
                "course-import-confirm", idempotencyKey, CourseImportResult.class,
                () -> courseImportService.confirm(request.token()));
        return ApiResponse.ok(result);
    }
}

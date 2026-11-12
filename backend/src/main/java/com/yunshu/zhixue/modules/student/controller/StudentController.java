package com.yunshu.zhixue.modules.student.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.student.dto.StudentBatchDeleteRequest;
import com.yunshu.zhixue.modules.student.dto.StudentBatchStatusRequest;
import com.yunshu.zhixue.modules.student.dto.StudentDetail;
import com.yunshu.zhixue.modules.student.dto.StudentInterviewItem;
import com.yunshu.zhixue.modules.student.dto.StudentItem;
import com.yunshu.zhixue.modules.student.dto.StudentStatusRequest;
import com.yunshu.zhixue.modules.student.service.StudentService;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学员管理（04 §5.8 A-U01 ~ A-U08）。
 */
@Tag(name = "学员管理", description = "A-U01 ~ A-U08")
@RestController
@RequestMapping("/api/admin/v1/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @Operation(summary = "A-U01 学员列表")
    @GetMapping
    @RequirePermission("student:view")
    public ApiResponse<PageResult<StudentItem>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(studentService.list(keyword, status, page, pageSize));
    }

    @Operation(summary = "A-U02 启用 / 禁用学员")
    @PutMapping("/{id}/status")
    @RequirePermission("student:edit")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody StudentStatusRequest request) {
        studentService.updateStatus(id, request.status());
        return ApiResponse.ok();
    }

    @Operation(summary = "A-U03 批量启用 / 禁用")
    @PutMapping("/batch-status")
    @RequirePermission("student:edit")
    public ApiResponse<Integer> batchStatus(
            @Valid @RequestBody StudentBatchStatusRequest request) {
        return ApiResponse.ok(studentService.batchStatus(request));
    }

    @Operation(summary = "A-U04 删除学员")
    @DeleteMapping("/{id}")
    @RequirePermission("student:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-U05 批量删除")
    @DeleteMapping("/batch")
    @RequirePermission("student:delete")
    public ApiResponse<Void> batchDelete(@Valid @RequestBody StudentBatchDeleteRequest request) {
        studentService.batchDelete(request.ids());
        return ApiResponse.ok();
    }

    @Operation(summary = "A-U06 学员详情")
    @GetMapping("/{id}/detail")
    @RequirePermission("student:view")
    public ApiResponse<StudentDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(studentService.detail(id));
    }

    @Operation(summary = "A-U07 学员面试记录")
    @GetMapping("/{id}/interviews")
    @RequirePermission("student:view")
    public ApiResponse<PageResult<StudentInterviewItem>> interviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "5") long pageSize) {
        return ApiResponse.ok(studentService.interviews(id, page, pageSize));
    }

    @Operation(summary = "A-U08 导出学员列表")
    @GetMapping("/export")
    @RequirePermission("student:view")
    public void export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) {
        studentService.export(response, keyword, status);
    }
}

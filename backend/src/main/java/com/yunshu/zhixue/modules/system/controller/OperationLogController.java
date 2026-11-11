package com.yunshu.zhixue.modules.system.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.system.dto.LogExport;
import com.yunshu.zhixue.modules.system.dto.OperationLogItem;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 操作日志（04 §5.11 A-G01 ~ A-G04）。
 */
@Tag(name = "操作日志", description = "A-G01 ~ A-G04")
@RestController
@RequestMapping("/api/admin/v1/logs")
public class OperationLogController {

    private final OperationLogService logService;

    public OperationLogController(OperationLogService logService) {
        this.logService = logService;
    }

    @Operation(summary = "A-G01 日志列表")
    @GetMapping
    @RequirePermission("system:log_view")
    public ApiResponse<PageResult<OperationLogItem>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(logService.list(keyword, operationType, startDate, endDate, page, pageSize));
    }

    @Operation(summary = "A-G02 日志详情")
    @GetMapping("/{id}")
    @RequirePermission("system:log_view")
    public ApiResponse<OperationLogItem> detail(@PathVariable Long id) {
        return ApiResponse.ok(logService.detail(id));
    }

    @Operation(summary = "A-G03 导出 CSV")
    @GetMapping("/export")
    @RequirePermission("system:log_view")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LogExport export = logService.export(keyword, operationType, startDate, endDate);
        String encodedName = URLEncoder.encode(export.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(export.content());
    }

    @Operation(summary = "A-G04 清空日志（物理删除）")
    @DeleteMapping
    @RequirePermission("system:log_view")
    public ApiResponse<Integer> clear() {
        return ApiResponse.ok(logService.clear());
    }
}

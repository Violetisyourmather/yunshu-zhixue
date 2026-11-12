package com.yunshu.zhixue.modules.question.controller;

import com.yunshu.zhixue.common.idempotency.IdempotencyService;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.question.dto.QuestionAuditRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionBatchDeleteRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionBatchResult;
import com.yunshu.zhixue.modules.question.dto.QuestionBatchStatusRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionDetail;
import com.yunshu.zhixue.modules.question.dto.QuestionImportConfirmRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionImportPreview;
import com.yunshu.zhixue.modules.question.dto.QuestionImportResult;
import com.yunshu.zhixue.modules.question.dto.QuestionItem;
import com.yunshu.zhixue.modules.question.dto.QuestionRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionStageGroup;
import com.yunshu.zhixue.modules.question.dto.QuestionStats;
import com.yunshu.zhixue.modules.question.dto.QuestionStatusRequest;
import com.yunshu.zhixue.modules.question.service.QuestionExportService;
import com.yunshu.zhixue.modules.question.service.QuestionImportService;
import com.yunshu.zhixue.modules.question.service.QuestionService;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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
 * 题库管理（04 §5.5 A-Q01 ~ A-Q15）。
 */
@Tag(name = "题库管理", description = "A-Q01 ~ A-Q15")
@RestController
@RequestMapping("/api/admin/v1/questions")
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionImportService questionImportService;
    private final QuestionExportService questionExportService;
    private final IdempotencyService idempotencyService;

    public QuestionController(QuestionService questionService,
                              QuestionImportService questionImportService,
                              QuestionExportService questionExportService,
                              IdempotencyService idempotencyService) {
        this.questionService = questionService;
        this.questionImportService = questionImportService;
        this.questionExportService = questionExportService;
        this.idempotencyService = idempotencyService;
    }

    @Operation(summary = "A-Q01 题库统计卡")
    @GetMapping("/stats")
    @RequirePermission("question:view")
    public ApiResponse<QuestionStats> stats() {
        return ApiResponse.ok(questionService.stats());
    }

    @Operation(summary = "题库表单 / 筛选的阶段分组")
    @GetMapping("/form-options")
    @RequirePermission("question:view")
    public ApiResponse<List<QuestionStageGroup>> formOptions() {
        return ApiResponse.ok(questionService.stageGroups());
    }

    @Operation(summary = "A-Q02 题目列表")
    @GetMapping
    @RequirePermission("question:view")
    public ApiResponse<PageResult<QuestionItem>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "asc") String sort,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(questionService.list(keyword, subjectId, stageId,
                type, difficulty, status, sort, page, pageSize));
    }

    @Operation(summary = "A-Q03 题目详情")
    @GetMapping("/{id}")
    @RequirePermission("question:view")
    public ApiResponse<QuestionDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(questionService.detail(id));
    }

    @Operation(summary = "A-Q04 新增题目")
    @PostMapping
    @RequirePermission("question:add")
    public ApiResponse<Long> create(@Valid @RequestBody QuestionRequest request) {
        return ApiResponse.ok(questionService.create(request));
    }

    @Operation(summary = "A-Q05 编辑题目")
    @PutMapping("/{id}")
    @RequirePermission("question:edit")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody QuestionRequest request) {
        questionService.update(id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-Q07 题目审核")
    @PostMapping("/{id}/audit")
    @RequirePermission("question:audit")
    public ApiResponse<Void> audit(@PathVariable Long id,
                                   @Valid @RequestBody QuestionAuditRequest request) {
        questionService.audit(id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-Q08 上架 / 下架")
    @PostMapping("/{id}/status")
    @RequirePermission("question:edit")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody QuestionStatusRequest request) {
        questionService.updateStatus(id, request.action());
        return ApiResponse.ok();
    }

    @Operation(summary = "A-Q09 重新提交审核")
    @PostMapping("/{id}/resubmit")
    @RequirePermission("question:edit")
    public ApiResponse<Void> resubmit(@PathVariable Long id) {
        questionService.resubmit(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-Q10 批量上架 / 下架")
    @PostMapping("/batch-status")
    @RequirePermission("question:edit")
    public ApiResponse<QuestionBatchResult> batchStatus(
            @Valid @RequestBody QuestionBatchStatusRequest request) {
        return ApiResponse.ok(questionService.batchStatus(request));
    }

    @Operation(summary = "A-Q11 批量删除")
    @PostMapping("/batch-delete")
    @RequirePermission("question:delete")
    public ApiResponse<Void> batchDelete(@Valid @RequestBody QuestionBatchDeleteRequest request) {
        questionService.batchDelete(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-Q06 删除题目")
    @DeleteMapping("/{id}")
    @RequirePermission("question:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-Q12 批量导入预览（Excel）")
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("question:import")
    public ApiResponse<QuestionImportPreview> preview(
            @RequestPart("file") MultipartFile file,
            @RequestParam("subjectId") Long subjectId) {
        return ApiResponse.ok(questionImportService.preview(file, subjectId));
    }

    @Operation(summary = "A-Q13 确认批量导入")
    @PostMapping("/import/confirm")
    @RequirePermission("question:import")
    public ApiResponse<QuestionImportResult> confirm(
            @Valid @RequestBody QuestionImportConfirmRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        QuestionImportResult result = idempotencyService.execute(
                "question-import-confirm", idempotencyKey, QuestionImportResult.class,
                () -> questionImportService.confirm(request.token()));
        return ApiResponse.ok(result);
    }

    @Operation(summary = "A-Q14 下载导入模板")
    @GetMapping("/import/template")
    @RequirePermission("question:import")
    public void template(HttpServletResponse response) {
        questionExportService.template(response);
    }

    @Operation(summary = "A-Q15 导出题库")
    @GetMapping("/export")
    @RequirePermission("question:export")
    public void export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "asc") String sort,
            HttpServletResponse response) {
        questionExportService.export(response, keyword, subjectId, stageId, type, difficulty, status, sort);
    }
}

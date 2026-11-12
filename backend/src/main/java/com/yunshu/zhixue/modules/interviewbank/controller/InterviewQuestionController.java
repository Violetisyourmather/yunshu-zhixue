package com.yunshu.zhixue.modules.interviewbank.controller;

import com.yunshu.zhixue.common.idempotency.IdempotencyService;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportConfirmRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportPreview;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportResult;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewMarkdownConfirmRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewMarkdownPreview;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewMarkdownResult;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewSearchResultItem;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewSearchTestRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionCreateRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionDetail;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionItem;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionUpdateRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewStats;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewSubjectOption;
import com.yunshu.zhixue.modules.interviewbank.service.InterviewImportService;
import com.yunshu.zhixue.modules.interviewbank.service.InterviewQuestionService;
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
 * 面试题库管理（04 §5.6 A-I01 ~ A-I08、A-I14 的 Excel 导入替代实现）。
 */
@Tag(name = "面试题库", description = "A-I01 ~ A-I05；Excel 批量导入")
@RestController
@RequestMapping("/api/admin/v1/interview-questions")
public class InterviewQuestionController {

    private final InterviewQuestionService questionService;
    private final InterviewImportService importService;
    private final IdempotencyService idempotencyService;

    public InterviewQuestionController(InterviewQuestionService questionService,
                                       InterviewImportService importService,
                                       IdempotencyService idempotencyService) {
        this.questionService = questionService;
        this.importService = importService;
        this.idempotencyService = idempotencyService;
    }

    @Operation(summary = "面试题库统计卡")
    @GetMapping("/stats")
    @RequirePermission("interviewbank:view")
    public ApiResponse<InterviewStats> stats() {
        return ApiResponse.ok(questionService.stats());
    }

    @Operation(summary = "面试题方向下拉")
    @GetMapping("/subject-options")
    @RequirePermission("interviewbank:view")
    public ApiResponse<List<InterviewSubjectOption>> subjectOptions() {
        return ApiResponse.ok(questionService.subjectOptions());
    }

    @Operation(summary = "A-I01 面试题列表")
    @GetMapping
    @RequirePermission("interviewbank:view")
    public ApiResponse<PageResult<InterviewQuestionItem>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(defaultValue = "false") boolean unclassified,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(questionService.list(keyword, subjectId, topicId,
                difficulty, unclassified, page, pageSize));
    }

    @Operation(summary = "A-I02 面试题详情")
    @GetMapping("/{id}")
    @RequirePermission("interviewbank:view")
    public ApiResponse<InterviewQuestionDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(questionService.detail(id));
    }

    @Operation(summary = "A-I03 手工录入面试题")
    @PostMapping
    @RequirePermission("interviewbank:upload")
    public ApiResponse<Long> create(@Valid @RequestBody InterviewQuestionCreateRequest request) {
        return ApiResponse.ok(questionService.create(request));
    }

    @Operation(summary = "A-I04 编辑面试题（仅主题 + 难度）")
    @PutMapping("/{id}")
    @RequirePermission("interviewbank:edit")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody InterviewQuestionUpdateRequest request) {
        questionService.update(id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-I05 删除面试题")
    @DeleteMapping("/{id}")
    @RequirePermission("interviewbank:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "面试题 Excel 导入预览")
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("interviewbank:upload")
    public ApiResponse<InterviewImportPreview> preview(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(importService.preview(file));
    }

    @Operation(summary = "面试题 Excel 确认导入")
    @PostMapping("/import/confirm")
    @RequirePermission("interviewbank:upload")
    public ApiResponse<InterviewImportResult> confirm(
            @Valid @RequestBody InterviewImportConfirmRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        InterviewImportResult result = idempotencyService.execute(
                "interview-import-confirm", idempotencyKey, InterviewImportResult.class,
                () -> importService.confirm(request.token()));
        return ApiResponse.ok(result);
    }

    @Operation(summary = "下载面试题导入模板")
    @GetMapping("/import/template")
    @RequirePermission("interviewbank:upload")
    public void template(HttpServletResponse response) {
        importService.template(response);
    }

    @Operation(summary = "Markdown 导入预览（按 # ~ ### 标题切题）")
    @PostMapping(value = "/import/markdown/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("interviewbank:upload")
    public ApiResponse<InterviewMarkdownPreview> previewMarkdown(
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(importService.previewMarkdown(file));
    }

    @Operation(summary = "Markdown 确认导入（整批指定方向/主题/难度/标签）")
    @PostMapping("/import/markdown/confirm")
    @RequirePermission("interviewbank:upload")
    public ApiResponse<InterviewMarkdownResult> confirmMarkdown(
            @Valid @RequestBody InterviewMarkdownConfirmRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        InterviewMarkdownResult result = idempotencyService.execute(
                "interview-markdown-confirm", idempotencyKey, InterviewMarkdownResult.class,
                () -> importService.confirmMarkdown(request));
        return ApiResponse.ok(result);
    }

    @Operation(summary = "A-I09 检索测试（验证解析/导入结果）")
    @PostMapping("/search-test")
    @RequirePermission("interviewbank:search_test")
    public ApiResponse<List<InterviewSearchResultItem>> searchTest(
            @Valid @RequestBody InterviewSearchTestRequest request) {
        return ApiResponse.ok(questionService.searchTest(request.keyword()));
    }
}

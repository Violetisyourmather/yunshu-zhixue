package com.yunshu.zhixue.modules.study.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.study.dto.PracticeStartResponse;
import com.yunshu.zhixue.modules.study.dto.WrongQuestionItem;
import com.yunshu.zhixue.modules.study.dto.WrongQuestionStats;
import com.yunshu.zhixue.modules.study.dto.WrongRedoRequest;
import com.yunshu.zhixue.modules.study.service.WrongQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学员端错题本（04 §4.5 P-W01 ~ P-W05）。
 */
@Tag(name = "学员错题本", description = "P-W01 统计 / P-W02 列表 / P-W03 重做 / P-W04 标记掌握 / P-W05 移除")
@RestController
@RequestMapping("/api/portal/v1/wrong-questions")
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;

    public WrongQuestionController(WrongQuestionService wrongQuestionService) {
        this.wrongQuestionService = wrongQuestionService;
    }

    @Operation(summary = "P-W01 错题统计")
    @GetMapping("/stats")
    public ApiResponse<WrongQuestionStats> stats() {
        return ApiResponse.ok(wrongQuestionService.stats());
    }

    @Operation(summary = "P-W02 错题列表")
    @GetMapping
    public ApiResponse<PageResult<WrongQuestionItem>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(wrongQuestionService.list(status, type, days, sort, page, pageSize));
    }

    @Operation(summary = "P-W03 错题重做组卷")
    @PostMapping("/redo-start")
    public ApiResponse<PracticeStartResponse> redoStart(@Valid @RequestBody WrongRedoRequest request) {
        return ApiResponse.ok(wrongQuestionService.redoStart(request));
    }

    @Operation(summary = "P-W04 标记已掌握")
    @PostMapping("/{id}/mastered")
    public ApiResponse<Void> markMastered(@PathVariable long id) {
        wrongQuestionService.markMastered(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "P-W05 移除错题")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable long id) {
        wrongQuestionService.remove(id);
        return ApiResponse.ok();
    }
}

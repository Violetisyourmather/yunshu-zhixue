package com.yunshu.zhixue.modules.study.controller;

import com.yunshu.zhixue.common.idempotency.IdempotencyService;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.study.dto.PracticeStartRequest;
import com.yunshu.zhixue.modules.study.dto.PracticeStartResponse;
import com.yunshu.zhixue.modules.study.dto.PracticeSubmitRequest;
import com.yunshu.zhixue.modules.study.dto.PracticeSubmitResponse;
import com.yunshu.zhixue.modules.study.service.PracticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学员端刷题（04 §4.4 P-P01 / P-P02）。
 */
@Tag(name = "学员刷题", description = "P-P01 开始练习 / P-P02 提交作答")
@RestController
@RequestMapping("/api/portal/v1/practice")
public class PracticeController {

    private final PracticeService practiceService;
    private final IdempotencyService idempotencyService;

    public PracticeController(PracticeService practiceService,
                              IdempotencyService idempotencyService) {
        this.practiceService = practiceService;
        this.idempotencyService = idempotencyService;
    }

    @Operation(summary = "P-P01 开始练习")
    @PostMapping("/start")
    public ApiResponse<PracticeStartResponse> start(@Valid @RequestBody PracticeStartRequest request) {
        return ApiResponse.ok(practiceService.start(request));
    }

    @Operation(summary = "P-P02 提交作答（客观题重判，简答题 AI 批改）")
    @PostMapping("/{practiceId}/submit")
    public ApiResponse<PracticeSubmitResponse> submit(
            @PathVariable String practiceId,
            @Valid @RequestBody PracticeSubmitRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        PracticeSubmitResponse result = idempotencyService.execute(
                "practice-submit", idempotencyKey, PracticeSubmitResponse.class,
                () -> practiceService.submit(practiceId, request));
        return ApiResponse.ok(result);
    }
}

package com.yunshu.zhixue.modules.question.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.question.dto.PortalQuestionDetail;
import com.yunshu.zhixue.modules.question.dto.PortalQuestionItem;
import com.yunshu.zhixue.modules.question.dto.PortalStageItem;
import com.yunshu.zhixue.modules.question.dto.PortalSubjectItem;
import com.yunshu.zhixue.modules.question.service.PortalQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学员端题库浏览（04 §4.3 P-B01 ~ P-B04）。
 *
 * <p>浏览接口公开；题目详情根据当前登录态裁剪答案与解析。</p>
 */
@Tag(name = "学员题库", description = "P-B01 学科 / P-B02 阶段 / P-B03 题目列表 / P-B04 题目详情")
@RestController
@RequestMapping("/api/portal/v1")
public class PortalQuestionController {

    private final PortalQuestionService questionService;

    public PortalQuestionController(PortalQuestionService questionService) {
        this.questionService = questionService;
    }

    @Operation(summary = "P-B01 学科列表（公开）")
    @GetMapping("/subjects")
    public ApiResponse<List<PortalSubjectItem>> subjects() {
        return ApiResponse.ok(questionService.subjects());
    }

    @Operation(summary = "P-B02 阶段列表（公开）")
    @GetMapping("/subjects/{subjectId}/stages")
    public ApiResponse<List<PortalStageItem>> stages(@PathVariable long subjectId) {
        return ApiResponse.ok(questionService.stages(subjectId));
    }

    @Operation(summary = "P-B03 题目列表（公开）")
    @GetMapping("/questions")
    public ApiResponse<PageResult<PortalQuestionItem>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(questionService.list(keyword, subjectId, stageId,
                type, difficulty, page, pageSize));
    }

    @Operation(summary = "P-B04 题目详情（公开，登录后返回答案与解析）")
    @GetMapping("/questions/{id}")
    public ApiResponse<PortalQuestionDetail> detail(@PathVariable long id) {
        return ApiResponse.ok(questionService.detail(id));
    }
}

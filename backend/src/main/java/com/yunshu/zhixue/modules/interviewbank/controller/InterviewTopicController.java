package com.yunshu.zhixue.modules.interviewbank.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTopicItem;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTopicRequest;
import com.yunshu.zhixue.modules.interviewbank.service.InterviewQuestionService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 面试题主题词表（A-I10 ~ A-I13）。
 */
@Tag(name = "面试题主题", description = "A-I10 ~ A-I13")
@RestController
@RequestMapping("/api/admin/v1/interview-topics")
public class InterviewTopicController {

    private final InterviewQuestionService questionService;

    public InterviewTopicController(InterviewQuestionService questionService) {
        this.questionService = questionService;
    }

    @Operation(summary = "A-I10 主题列表（含 0 题主题）")
    @GetMapping
    @RequirePermission("interviewbank:view")
    public ApiResponse<List<InterviewTopicItem>> list() {
        return ApiResponse.ok(questionService.topics());
    }

    @Operation(summary = "A-I11 新增主题")
    @PostMapping
    @RequirePermission("interviewbank:edit")
    public ApiResponse<Long> create(@Valid @RequestBody InterviewTopicRequest request) {
        return ApiResponse.ok(questionService.createTopic(request));
    }

    @Operation(summary = "A-I12 编辑主题")
    @PutMapping("/{id}")
    @RequirePermission("interviewbank:edit")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody InterviewTopicRequest request) {
        questionService.updateTopic(id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-I13 删除主题")
    @DeleteMapping("/{id}")
    @RequirePermission("interviewbank:edit")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        questionService.deleteTopic(id);
        return ApiResponse.ok();
    }
}

package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * A-Q11 批量删除题目。
 */
public record QuestionBatchDeleteRequest(
        @NotEmpty(message = "请选择题目")
        List<Long> ids) {
}

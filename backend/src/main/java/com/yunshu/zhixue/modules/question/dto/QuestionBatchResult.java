package com.yunshu.zhixue.modules.question.dto;

/**
 * 批量状态流转结果：实际更新数与被跳过的题目数。
 */
public record QuestionBatchResult(int updated, int skipped) {
}

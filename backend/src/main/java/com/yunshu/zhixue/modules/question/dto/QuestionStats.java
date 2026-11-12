package com.yunshu.zhixue.modules.question.dto;

/**
 * A-Q01 题库统计卡（7 项）。
 */
public record QuestionStats(
        long total,
        long online,
        long pending,
        long singleChoice,
        long multipleChoice,
        long judge,
        long shortAnswer) {
}

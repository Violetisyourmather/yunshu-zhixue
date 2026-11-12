package com.yunshu.zhixue.modules.question.dto;

/**
 * P-B03 题库浏览题目卡（公开，不含选项、答案与解析）。
 *
 * @param doneCount    作答人次
 * @param accuracyRate 正确率（0~100，保留 1 位小数）
 */
public record PortalQuestionItem(
        long id,
        int type,
        int difficulty,
        String content,
        String subjectName,
        String stageName,
        String tags,
        long doneCount,
        double accuracyRate) {
}

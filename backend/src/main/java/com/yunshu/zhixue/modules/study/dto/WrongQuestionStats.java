package com.yunshu.zhixue.modules.study.dto;

import java.util.List;

/**
 * P-W01 错题统计。
 *
 * @param masteryRate 掌握率百分比（0~100，保留 1 位小数）
 * @param top3        错误次数最多的待复习错题
 */
public record WrongQuestionStats(
        long total,
        long toReviewCount,
        long masteredCount,
        double masteryRate,
        List<WrongTopItem> top3) {

    /**
     * @param id         错题记录 id（重做时使用）
     * @param questionId 题目 id
     */
    public record WrongTopItem(long id, long questionId, String content, long errorCount) {
    }
}

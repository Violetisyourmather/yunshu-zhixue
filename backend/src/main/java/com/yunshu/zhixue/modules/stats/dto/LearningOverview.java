package com.yunshu.zhixue.modules.stats.dto;

import java.util.List;

/**
 * P-H03 首页学习概览（登录）。
 *
 * @param learningStats 学习统计
 * @param weekTrend     近 7 天练习量，按日期升序，最后一项为今天
 * @param todayCount    今日练习量
 * @param guideFinished 新手引导是否已完成
 */
public record LearningOverview(
        LearningStats learningStats,
        List<WeekTrendPoint> weekTrend,
        long todayCount,
        boolean guideFinished) {
}

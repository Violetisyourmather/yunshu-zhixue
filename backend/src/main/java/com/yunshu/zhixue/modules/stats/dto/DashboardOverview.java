package com.yunshu.zhixue.modules.stats.dto;

/**
 * A-D01 数据看板统计卡。
 *
 * <p>每项指标包含总量与本自然月新增量（delta），前端展示为「较上月 +N」。
 * 自然月按 Asia/Shanghai 划分。</p>
 */
public record DashboardOverview(
        long subjectCount,
        long subjectDelta,
        long questionCount,
        long questionDelta,
        long studentCount,
        long studentDelta,
        long interviewCount,
        long interviewDelta) {
}

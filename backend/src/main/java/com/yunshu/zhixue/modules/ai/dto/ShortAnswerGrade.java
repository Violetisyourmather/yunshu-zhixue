package com.yunshu.zhixue.modules.ai.dto;

import java.util.List;

/**
 * 简答题 AI 评分结果。
 *
 * @param score    0~10
 * @param feedback 评语
 * @param points   关键点命中情况
 * @param degraded AI 不可用时为 true，表示使用了本地降级评分
 */
public record ShortAnswerGrade(
        double score,
        String feedback,
        List<AiPoint> points,
        boolean degraded) {
}

package com.yunshu.zhixue.modules.study.dto;

import com.yunshu.zhixue.modules.ai.dto.AiPoint;

import java.util.List;

/**
 * P-P02 单题判分结果。
 *
 * @param score          客观题为 1 / 0，简答为 0~10
 * @param answer         正确答案：选择题为选项标识，判断 / 简答为答案文本
 * @param aiFeedback     简答题 AI 评语
 * @param referencePoints 简答题关键点命中情况
 * @param degraded       AI 不可用时的降级评分标记
 */
public record PracticeResultItem(
        long questionId,
        boolean isCorrect,
        double score,
        String answer,
        String analysis,
        String aiFeedback,
        List<AiPoint> referencePoints,
        boolean degraded) {
}

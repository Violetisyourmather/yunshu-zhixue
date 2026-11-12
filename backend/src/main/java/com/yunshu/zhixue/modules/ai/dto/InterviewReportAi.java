package com.yunshu.zhixue.modules.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * AI 生成的面试复盘报告。
 *
 * @param overallScore 按六维权重计算后的综合分（0~10）
 */
public record InterviewReportAi(
        Map<String, Double> dimensions,
        List<String> strengths,
        List<String> weaknesses,
        List<String> suggestions,
        List<InterviewImproveCard> improveCards,
        double overallScore,
        boolean degraded) {
}

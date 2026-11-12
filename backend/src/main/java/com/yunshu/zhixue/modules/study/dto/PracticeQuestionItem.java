package com.yunshu.zhixue.modules.study.dto;

import java.util.List;

/**
 * P-P01 返回给前端的练习题目。
 *
 * <p>答案随题返回用于前端即时判定；服务端在 P-P02 会重新判分，前后端答案不一致时以服务端为准。</p>
 */
public record PracticeQuestionItem(
        long id,
        int type,
        int difficulty,
        String content,
        List<PracticeOptionItem> options,
        String answer,
        String analysis,
        String stageName,
        String subjectName,
        String tags,
        long doneCount,
        double accuracyRate) {
}

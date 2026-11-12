package com.yunshu.zhixue.modules.study.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * P-W02 错题列表项。
 *
 * @param userAnswer   最近一次错误作答快照
 * @param correctAnswer 组装后的正确答案
 * @param lastWrongAt  最近答错时间（序列化为 Asia/Shanghai）
 */
public record WrongQuestionItem(
        long id,
        long questionId,
        int type,
        String content,
        List<PracticeOptionItem> options,
        String correctAnswer,
        String userAnswer,
        String analysis,
        int errorCount,
        int correctedCount,
        int status,
        LocalDateTime lastWrongAt,
        String subjectName,
        String stageName) {
}

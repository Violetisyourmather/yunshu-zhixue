package com.yunshu.zhixue.modules.study.dto;

import java.util.List;

/**
 * P-P01 开始练习响应。
 */
public record PracticeStartResponse(
        String practiceId,
        String title,
        List<PracticeQuestionItem> questions) {
}

package com.yunshu.zhixue.modules.question.dto;

import java.util.List;

/**
 * 按学科分组的阶段下拉数据。
 */
public record QuestionStageGroup(
        long subjectId,
        String subjectName,
        List<QuestionStageOption> stages) {
}

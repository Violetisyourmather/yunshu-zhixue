package com.yunshu.zhixue.modules.study.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * P-P02 单题作答。
 *
 * <p>{@code answer}：单选为选项标识；多选为逗号分隔的标识集合；判断为「正确 / 错误」；
 * 简答为作答文本。</p>
 */
public record PracticeAnswer(
        @NotNull(message = "题目 id 不能为空")
        Long questionId,

        String answer,

        @Min(value = 0, message = "答题用时不能为负数")
        Integer durationSeconds) {
}

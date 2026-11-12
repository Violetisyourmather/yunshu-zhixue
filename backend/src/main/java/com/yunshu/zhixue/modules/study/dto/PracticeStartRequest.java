package com.yunshu.zhixue.modules.study.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * P-P01 开始练习入参。
 */
public record PracticeStartRequest(
        @NotBlank(message = "练习来源不能为空")
        @Pattern(regexp = "question|subject|wrong_redo", message = "练习来源不合法")
        String source,

        Long questionId,

        Long subjectId,

        Long stageId,

        Integer type,

        Integer difficulty,

        @Min(value = 1, message = "题量至少为 1")
        @Max(value = 100, message = "单次最多 100 题")
        Integer count,

        @NotNull(message = "请选择练习模式")
        @Min(value = 1, message = "练习模式不合法")
        @Max(value = 2, message = "练习模式不合法")
        Integer mode) {
}

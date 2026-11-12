package com.yunshu.zhixue.modules.study.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * P-P02 提交作答入参。
 *
 * <p>前端通常逐题提交；也支持一次提交多题。{@code mode} 用于作答明细落库。</p>
 */
public record PracticeSubmitRequest(
        @NotEmpty(message = "请提交作答")
        @Size(max = 50, message = "单次最多提交 50 道题")
        List<@Valid PracticeAnswer> answers,

        @Min(value = 1, message = "练习模式不合法")
        @Max(value = 2, message = "练习模式不合法")
        Integer mode) {
}

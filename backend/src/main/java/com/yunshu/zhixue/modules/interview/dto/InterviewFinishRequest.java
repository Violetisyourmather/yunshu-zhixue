package com.yunshu.zhixue.modules.interview.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * P-I04 结束面试入参；未作答题目保留 order 并以 null 作答提交。
 */
public record InterviewFinishRequest(
        @NotNull(message = "作答列表不能为空")
        @Size(max = 20, message = "单次面试最多 20 道题")
        List<@Valid InterviewAnswerRequest> answers) {
}

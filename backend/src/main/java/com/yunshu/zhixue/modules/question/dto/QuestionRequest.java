package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * A-Q04 / A-Q05 新增或编辑题目。
 *
 * @param options     选择题选项（单选 / 多选必填，至少 2 项）
 * @param answerKeys  选择题正确项标识（如 A、B、C）
 * @param answer      判断题「正确/错误」或简答题参考答案
 */
public record QuestionRequest(
        @NotNull(message = "请选择阶段")
        Long stageId,

        @NotNull(message = "请选择题型")
        @Min(value = 1, message = "题型不合法")
        @Max(value = 4, message = "题型不合法")
        Integer type,

        @NotNull(message = "请选择难度")
        @Min(value = 1, message = "难度不合法")
        @Max(value = 3, message = "难度不合法")
        Integer difficulty,

        @NotBlank(message = "题目内容不能为空")
        @Size(max = 10000, message = "题目内容不能超过 10000 个字符")
        String content,

        @Size(max = 5000, message = "解析不能超过 5000 个字符")
        String analysis,

        @Size(max = 255, message = "标签不能超过 255 个字符")
        String tags,

        List<QuestionOptionRequest> options,

        List<String> answerKeys,

        @Size(max = 10000, message = "答案不能超过 10000 个字符")
        String answer) {
}

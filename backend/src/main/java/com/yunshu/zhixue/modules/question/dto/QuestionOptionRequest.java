package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.Size;

/**
 * 新增 / 编辑题目的选项。
 */
public record QuestionOptionRequest(
        @Size(max = 4, message = "选项标识不能超过 4 个字符")
        String key,

        @Size(max = 500, message = "选项内容不能超过 500 个字符")
        String text) {
}

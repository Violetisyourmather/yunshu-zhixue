package com.yunshu.zhixue.modules.study.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * P-W03 错题重做入参，ids 为 wrong_question.id。
 */
public record WrongRedoRequest(
        @NotEmpty(message = "请选择要重做的错题")
        @Size(max = 100, message = "单次最多重做 100 道错题")
        List<Long> ids) {
}

package com.yunshu.zhixue.modules.student.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * A-U05 批量删除学员。
 */
public record StudentBatchDeleteRequest(
        @NotEmpty(message = "请选择学员")
        List<Long> ids) {
}

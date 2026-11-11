package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * A-M06 批量删除后台用户。
 */
public record BatchDeleteRequest(
        @NotEmpty(message = "请选择用户")
        List<Long> ids) {
}

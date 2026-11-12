package com.yunshu.zhixue.modules.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * A-S05 / A-S09 上移或下移。
 */
public record SortRequest(
        @NotNull(message = "请选择要排序的记录")
        Long id,

        @NotBlank(message = "请选择移动方向")
        @Pattern(regexp = "up|down", message = "移动方向仅支持 up / down")
        String move) {
}

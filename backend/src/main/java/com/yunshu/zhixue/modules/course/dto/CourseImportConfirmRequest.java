package com.yunshu.zhixue.modules.course.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * A-C07 确认导入：携带预览令牌，避免重复上传文件。
 */
public record CourseImportConfirmRequest(
        @NotBlank(message = "导入令牌不能为空")
        String token) {
}

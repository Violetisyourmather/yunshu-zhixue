package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A-T02 保存系统基本信息。
 */
public record SystemSettingsRequest(
        @NotBlank(message = "平台名称不能为空")
        @Size(max = 100, message = "平台名称不能超过 100 个字符")
        String siteName,

        @Size(max = 500, message = "平台描述不能超过 500 个字符")
        String siteDescription,

        @Size(max = 500, message = "Logo 地址不能超过 500 个字符")
        String logoUrl,

        @Size(max = 500, message = "默认头像地址不能超过 500 个字符")
        String defaultAvatar) {
}

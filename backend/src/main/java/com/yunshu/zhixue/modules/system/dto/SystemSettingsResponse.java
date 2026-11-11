package com.yunshu.zhixue.modules.system.dto;

/**
 * A-T01 系统基本信息。
 */
public record SystemSettingsResponse(
        String siteName,
        String siteDescription,
        String logoUrl,
        String defaultAvatar) {
}

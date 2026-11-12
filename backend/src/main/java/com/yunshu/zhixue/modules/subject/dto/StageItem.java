package com.yunshu.zhixue.modules.subject.dto;

/**
 * 学科卡片内的阶段条目（A-S02）。
 *
 * @param status 1 启用 / 0 停用
 */
public record StageItem(
        long id,
        String name,
        String icon,
        int sortOrder,
        int status,
        long questionCount) {
}

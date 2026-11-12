package com.yunshu.zhixue.modules.question.dto;

/**
 * P-B01 题库学科卡（公开）。
 */
public record PortalSubjectItem(
        long id,
        String name,
        String icon,
        String description,
        long stageCount,
        int sortOrder) {
}

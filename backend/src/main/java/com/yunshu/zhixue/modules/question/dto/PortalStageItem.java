package com.yunshu.zhixue.modules.question.dto;

/**
 * P-B02 学科下的阶段卡（公开）。
 *
 * @param questionCount 该阶段已上架题目数
 */
public record PortalStageItem(
        long id,
        String name,
        String icon,
        long questionCount,
        int sortOrder) {
}

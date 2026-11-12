package com.yunshu.zhixue.modules.ai.dto;

/**
 * 简答题评分关键点。
 *
 * @param point 关键点描述
 * @param hit   学员答案是否命中
 */
public record AiPoint(String point, boolean hit) {
}

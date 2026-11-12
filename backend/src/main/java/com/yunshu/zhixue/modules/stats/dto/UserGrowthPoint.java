package com.yunshu.zhixue.modules.stats.dto;

/**
 * A-D02 用户增长曲线日粒度数据点（上海自然日）。
 *
 * @param date  日期，格式 {@code yyyy-MM-dd}
 * @param count 当日新增注册用户数
 */
public record UserGrowthPoint(String date, long count) {
}

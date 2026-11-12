package com.yunshu.zhixue.modules.stats.dto;

/**
 * 近 7 天练习量中的单日数据（上海自然日）。
 *
 * @param date  日期，格式 yyyy-MM-dd
 * @param count 当日作答记录数
 */
public record WeekTrendPoint(String date, long count) {
}

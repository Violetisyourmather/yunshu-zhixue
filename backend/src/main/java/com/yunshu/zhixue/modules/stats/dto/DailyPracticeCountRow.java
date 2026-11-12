package com.yunshu.zhixue.modules.stats.dto;

import lombok.Data;

/**
 * 按上海自然日聚合的练习量查询行。
 */
@Data
public class DailyPracticeCountRow {

    /** 日期，格式 yyyy-MM-dd。 */
    private String practiceDate;

    private Long total;
}

package com.yunshu.zhixue.modules.stats.dto;

import lombok.Data;

/**
 * 按日聚合行（看板用户增长曲线）。
 */
@Data
public class DailyCountRow {

    /** 上海自然日，格式 yyyy-MM-dd。 */
    private String day;

    private Long total;
}

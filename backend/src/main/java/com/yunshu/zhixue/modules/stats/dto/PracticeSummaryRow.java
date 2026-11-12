package com.yunshu.zhixue.modules.stats.dto;

import lombok.Data;

/**
 * 学员刷题汇总行：总记录数、客观题作答数、客观题答对数。
 *
 * <p>简答题（type=4）按「通过 / 不通过」单列，不计入正确率分母。</p>
 */
@Data
public class PracticeSummaryRow {

    private Long total;

    private Long objectiveTotal;

    private Long objectiveCorrect;
}

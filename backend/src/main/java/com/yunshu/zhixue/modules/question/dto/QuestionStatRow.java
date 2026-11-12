package com.yunshu.zhixue.modules.question.dto;

import lombok.Data;

/**
 * 题目作答统计聚合行（practice_record 按题目聚合）。
 */
@Data
public class QuestionStatRow {

    private Long id;

    private Long total;

    private Long correct;
}

package com.yunshu.zhixue.modules.student.dto;

import lombok.Data;

/**
 * 学员刷题聚合行：作答总数与正确数。
 */
@Data
public class StudentPracticeStatRow {

    private Long id;

    private Long total;

    private Long correct;
}

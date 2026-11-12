package com.yunshu.zhixue.modules.student.dto;

import lombok.Data;

/**
 * 学员维度计数聚合行（学习天数 / 面试次数 / 已完成课程数）。
 */
@Data
public class StudentCountRow {

    private Long id;

    private Long total;
}

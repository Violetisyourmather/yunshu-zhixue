package com.yunshu.zhixue.modules.subject.dto;

/**
 * A-S01 学科管理统计卡（需求 4.3.1）：总学科数 / 有阶段的学科 / 阶段总数 / 题目总数。
 */
public record SubjectStats(
        long subjectCount,
        long subjectWithStages,
        long stageCount,
        long questionCount) {
}

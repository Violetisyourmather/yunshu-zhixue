package com.yunshu.zhixue.modules.course.dto;

/**
 * A-C01 课程库统计卡：课程总数 / 总时长 / 关联学科数（「其他学科」不计入）。
 *
 * @param totalHours 总时长（小时，保留 1 位小数）
 */
public record CourseStats(
        long courseCount,
        long totalMinutes,
        double totalHours,
        long subjectCount) {
}

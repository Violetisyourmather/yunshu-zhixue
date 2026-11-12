package com.yunshu.zhixue.modules.student.dto;

import java.time.LocalDateTime;

/**
 * A-U06 学员详情：基本资料 + 学习统计 + 面试次数。
 */
public record StudentDetail(
        long id,
        String nickname,
        String mobile,
        String email,
        String avatar,
        int gender,
        int status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        long learningDays,
        long completedCourseCount,
        long practiceCount,
        double accuracyRate,
        long interviewCount) {
}

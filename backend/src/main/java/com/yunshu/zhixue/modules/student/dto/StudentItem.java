package com.yunshu.zhixue.modules.student.dto;

import java.time.LocalDateTime;

/**
 * A-U01 学员列表项。
 */
public record StudentItem(
        long id,
        String nickname,
        String mobile,
        String avatar,
        int status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        long learningDays,
        long practiceCount,
        long interviewCount) {
}

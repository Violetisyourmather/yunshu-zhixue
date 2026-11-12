package com.yunshu.zhixue.modules.course.dto;

import java.util.List;

/**
 * A-C02 课程列表项。
 *
 * @param subjectId 主学科；{@code null} 表示「其他学科」
 * @param subjects  全部关联学科（主学科在前），为空即「其他学科」
 */
public record CourseItem(
        long id,
        String name,
        String coverUrl,
        String intro,
        int durationMinutes,
        int source,
        String link,
        String bvid,
        int status,
        Long subjectId,
        List<SubjectRef> subjects) {
}

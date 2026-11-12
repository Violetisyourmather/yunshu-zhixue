package com.yunshu.zhixue.modules.route.dto;

import com.yunshu.zhixue.modules.course.dto.SubjectRef;

import java.util.List;

/**
 * A-R06 路线详情中的课程条目。
 *
 * @param routeCourseId 路线-课程关联 id（排序与移除使用）
 * @param sameSubject   课程是否归属于路线所选学科（否则为跨学科补充）
 */
public record RouteCourseItem(
        long routeCourseId,
        long courseId,
        String name,
        String bvid,
        int durationMinutes,
        int status,
        List<SubjectRef> subjects,
        boolean sameSubject,
        int sortOrder) {
}

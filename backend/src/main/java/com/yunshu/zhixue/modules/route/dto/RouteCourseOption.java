package com.yunshu.zhixue.modules.route.dto;

import com.yunshu.zhixue.modules.course.dto.SubjectRef;

import java.util.List;

/**
 * A-R07 「从课程库添加」候选课程。
 *
 * @param sameSubject 课程是否归属于路线所选学科
 * @param added       是否已在当前路线中
 */
public record RouteCourseOption(
        long courseId,
        String name,
        String bvid,
        int durationMinutes,
        int status,
        List<SubjectRef> subjects,
        boolean sameSubject,
        boolean added) {
}

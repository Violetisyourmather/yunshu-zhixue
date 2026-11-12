package com.yunshu.zhixue.modules.route.dto;

/**
 * P-R02 学习路线中的课程。
 *
 * @param routeCourseId 路线-课程关联 id，完成标记使用
 * @param courseId      课程 id
 * @param completed     当前学员是否已标记完成
 */
public record PortalRouteCourseItem(
        long routeCourseId,
        long courseId,
        String name,
        String bvid,
        int durationMinutes,
        int source,
        String coverUrl,
        boolean completed,
        int sortOrder) {
}

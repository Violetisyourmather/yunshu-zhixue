package com.yunshu.zhixue.modules.route.dto;

/**
 * 学员学习路线页的学科选择卡。
 *
 * @param courseCount 该学科上架课程数
 * @param routeId     该学科启用中的路线 id；null 表示暂无路线
 * @param routeName   路线名称；null 表示暂无路线
 */
public record PortalRouteSubjectItem(
        long subjectId,
        String subjectName,
        String icon,
        long courseCount,
        Long routeId,
        String routeName) {
}

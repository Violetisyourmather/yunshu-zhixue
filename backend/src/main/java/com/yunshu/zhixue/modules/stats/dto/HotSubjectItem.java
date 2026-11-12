package com.yunshu.zhixue.modules.stats.dto;

/**
 * P-H02 首页热门学科卡片。
 *
 * @param subjectId     学科 id
 * @param subjectName   学科名称
 * @param icon          图标（Emoji）
 * @param stageCount    启用阶段数
 * @param questionCount 阶段下未删除题目数
 */
public record HotSubjectItem(
        long subjectId,
        String subjectName,
        String icon,
        long stageCount,
        long questionCount) {
}

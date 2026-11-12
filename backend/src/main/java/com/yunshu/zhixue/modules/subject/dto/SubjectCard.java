package com.yunshu.zhixue.modules.subject.dto;

import java.util.List;

/**
 * A-S02 学科卡片：基础信息 + 课程 / 路线 / 题目统计 + 阶段列表。
 *
 * <p>阶段列表返回全部未删除阶段（含停用），管理端需要据此完成启停与排序；
 * 学员端自行按 {@code status=1} 过滤。</p>
 */
public record SubjectCard(
        long id,
        String name,
        String icon,
        String description,
        String themeColor,
        int sortOrder,
        long courseCount,
        long routeCount,
        long questionCount,
        List<StageItem> stages) {
}

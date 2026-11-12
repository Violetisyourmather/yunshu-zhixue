package com.yunshu.zhixue.modules.stats.dto;

/**
 * A-D03 题目学科分布项。
 *
 * @param subjectId     学科 id
 * @param subjectName   学科名称
 * @param questionCount 该学科下未删除题目数
 */
public record QuestionDistributionItem(long subjectId, String subjectName, long questionCount) {
}

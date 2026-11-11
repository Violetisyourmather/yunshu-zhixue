/**
 * 数据统计模块（S4 + A1 数据看板）。
 *
 * <p>职责：为首页、个人中心、数据看板、学员详情提供统一统计口径——
 * 总练习数、正确率、连续/累计学习天数、已学课程数、面试次数与分数、
 * 看板四项指标与趋势、题目学科分布、最近活动。</p>
 *
 * <p>口径：明细流水（{@code practice_record} / {@code interview_record} /
 * {@code route_course_completion}）为事实来源，查询时聚合；热点结果 Redis 缓存 5 分钟。</p>
 *
 * <p>边界：只读，不参与任何业务写入；不直接访问其他模块的 Mapper，
 * 跨模块取数经对方 Service 接口。</p>
 */
package com.yunshu.zhixue.modules.stats;

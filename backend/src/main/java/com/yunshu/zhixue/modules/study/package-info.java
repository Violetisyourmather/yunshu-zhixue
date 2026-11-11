/**
 * 学习训练模块（P2 刷题与错题）。
 *
 * <p>数据表：{@code practice_record}、{@code wrong_question}。</p>
 *
 * <p>职责：顺序/随机练习、客观题即时判分、简答题 AI 批改（经 {@code ai} 模块，
 * ≥6 分通过）、作答明细分片落库、错题归集与错误等级、连续答对 3 次转已掌握、
 * 错题重做与批量操作。</p>
 *
 * <p>边界：题目内容归 {@code question}；未完成练习进度仅存前端本地（不落库）；
 * 统计口径由 {@code stats} 基于作答明细聚合。</p>
 */
package com.yunshu.zhixue.modules.study;

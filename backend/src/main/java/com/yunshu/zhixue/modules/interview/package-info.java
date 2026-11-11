/**
 * AI 面试模块（P-I01~P-I05、P-I07~P-I08）。
 *
 * <p>数据表：{@code interview_record}、{@code interview_question_score}、
 * {@code interview_report}；会话过程暂存 Redis（{@code interview:temp:{sessionId}}，TTL 24h）。</p>
 *
 * <p>职责：配置校验与每日配额（5 次/天，上海日界）、按方向（=学科）+ 难度抽题、
 * 逐题评分（SSE 流式）、结束时单事务落库报告、面试记录与回放。</p>
 *
 * <p>边界：不维护面试题（归 {@code interviewbank}）；未完成会话不落业务库（D16）；
 * 报告不提供下载与分享。</p>
 */
package com.yunshu.zhixue.modules.interview;

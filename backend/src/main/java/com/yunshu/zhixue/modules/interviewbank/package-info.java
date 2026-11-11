/**
 * 面试题库模块（A-I01~A-I14）。
 *
 * <p>数据表：{@code interview_question}、{@code interview_topic}。
 * 题目按方向归属，方向 = 学科（{@code subject_id}），无独立「知识库」实体。</p>
 *
 * <p>职责：跨方向总览与筛选、手工录入、Markdown 导入（按 {@code #}~{@code ###} 切题，
 * 预览后批量入库）、主题词表维护、上下架、检索测试。</p>
 *
 * <p>边界：出题与评分归 {@code interview}；上传的 Markdown 文件不入库，
 * 仅保留来源文档名与题号。</p>
 */
package com.yunshu.zhixue.modules.interviewbank;

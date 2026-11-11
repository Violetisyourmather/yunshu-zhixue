/**
 * 题库模块（A2 题库管理 + 学员端题库浏览）。
 *
 * <p>数据表：{@code question}、{@code question_option}。</p>
 *
 * <p>职责：题目 CRUD、选项与正确答案校验、审核状态机（待审核 → 已上架/已驳回，
 * 上下架流转）、Excel 导入导出、标签与答题统计聚合（题目详情）。</p>
 *
 * <p>边界：判分与作答记录归 {@code study}；学员端只读已上架题目；
 * 简答题需「参考答案 + 关键点」两个字段。</p>
 */
package com.yunshu.zhixue.modules.question;

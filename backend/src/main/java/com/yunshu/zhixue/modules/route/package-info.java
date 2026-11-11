/**
 * 学习路线模块（A-R01~A-R09 + 学员端学习路线）。
 *
 * <p>数据表：{@code learning_route}、{@code route_course}、{@code route_course_completion}。</p>
 *
 * <p>职责：路线 CRUD（单学科归属）、课程编排与排序、从课程库添加（含跨学科补充）、
 * 启停、学员端展示与「标记完成」勾选（计入已学课程）。</p>
 *
 * <p>边界：不记录观看进度与时长，完成状态仅由学员手动勾选产生；
 * 课程数据归 {@code course}。</p>
 */
package com.yunshu.zhixue.modules.route;

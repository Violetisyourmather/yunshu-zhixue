/**
 * B 站课程库模块（A-C01~A-C08）。
 *
 * <p>数据表：{@code course}、{@code course_subject}。课程与学科多对多：
 * 主学科存 {@code course.subject_id}（NULL = 「其他学科」兜底），额外学科存关联表。</p>
 *
 * <p>职责：课程 CRUD、多学科关联、上下架、批量导入（预览 + 校验 + 确认）。</p>
 *
 * <p>边界：不记录观看进度与播放量（D4-7）；学员端只消费上架课程；
 * 路线引用由 {@code route} 模块负责。</p>
 */
package com.yunshu.zhixue.modules.course;

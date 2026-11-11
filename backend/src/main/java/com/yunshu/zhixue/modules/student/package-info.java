/**
 * 学员模块（P4 个人中心 + A3 学员管理）。
 *
 * <p>数据表：{@code student}；学习数据由 {@code study} / {@code interview} / {@code route}
 * 的记录聚合，本模块不重复存储。</p>
 *
 * <p>职责：本人资料维护（头像、昵称、邮箱、性别、技术方向偏好、引导状态）；
 * 后台学员列表、详情、启禁用、批量操作、导出。</p>
 *
 * <p>边界：学员由前台注册产生，后台不新增/编辑学员（需求 4.4）。</p>
 */
package com.yunshu.zhixue.modules.student;

package com.yunshu.zhixue.modules.route.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 路线-课程关联（03 §4.2 route_course）：顺序即学员的学习顺序。
 *
 * <p>{@code uk_route_course(route_id, course_id)} 唯一；移除课程采用逻辑删除，
 * 重新添加同一课程时复用并恢复原行，避免唯一键冲突。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("route_course")
public class RouteCourse extends BaseEntity {

    /** → learning_route.id */
    private Long routeId;

    /** → course.id */
    private Long courseId;

    private Integer sortOrder;
}

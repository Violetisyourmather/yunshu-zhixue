package com.yunshu.zhixue.modules.route.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 学员课程完成记录（03 route_course_completion）。
 *
 * <p>平台不记录观看进度与时长，仅记录学员手动勾选「标记完成」的时间；
 * 取消完成采用逻辑删除，重新标记时恢复原行以复用唯一键。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("route_course_completion")
public class RouteCourseCompletion extends BaseEntity {

    /** → student.id */
    private Long studentId;

    /** → route_course.id */
    private Long routeCourseId;

    /** 完成时间（UTC）。 */
    private LocalDateTime completedAt;
}

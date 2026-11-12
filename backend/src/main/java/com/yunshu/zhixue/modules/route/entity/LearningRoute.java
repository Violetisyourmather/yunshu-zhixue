package com.yunshu.zhixue.modules.route.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学习路线（03 §4.2 learning_route）：单学科归属，课程顺序由 route_course 维护。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("learning_route")
public class LearningRoute extends BaseEntity {

    private String name;

    /** → subject.id（单学科） */
    private Long subjectId;

    /** 1 启用 / 0 停用 */
    private Integer status;

    private String icon;

    /** 适合学历（自由文本，常用值见前端下拉） */
    private String targetEducation;

    private String description;
}

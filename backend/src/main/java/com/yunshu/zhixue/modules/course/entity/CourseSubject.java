package com.yunshu.zhixue.modules.course.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程-学科关联（03 §4.2 course_subject）：仅存额外学科，主学科在 {@code course.subject_id}。
 *
 * <p>纯关联数据，编辑时按课程整体覆盖（物理删除旧关联后重建），
 * 避免逻辑删除行占用 {@code uk_course_subject} 唯一键。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_subject")
public class CourseSubject extends BaseEntity {

    /** → course.id */
    private Long courseId;

    /** → subject.id */
    private Long subjectId;
}

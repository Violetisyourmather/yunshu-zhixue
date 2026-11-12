package com.yunshu.zhixue.modules.course.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程（03 §4.2 course）。
 *
 * <p>一门课可归属多个学科：{@code subjectId} 为主学科（NULL = 其他学科），
 * 额外学科存 {@code course_subject}。B 站来源的 {@code bvid} 全局唯一。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course")
public class Course extends BaseEntity {

    private String name;

    private String coverUrl;

    private String intro;

    /** 时长（分钟）。 */
    private Integer durationMinutes;

    /** 1 B站 / 2 其他 */
    private Integer source;

    private String link;

    /** BV 号（非 B 站来源为空）。 */
    private String bvid;

    /** 1 上架 / 2 下架 */
    private Integer status;

    /** 主学科 → subject.id；NULL = 其他学科 */
    private Long subjectId;
}

package com.yunshu.zhixue.modules.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 面试记录（03 interview_record）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interview_record")
public class InterviewRecord extends BaseEntity {

    private Long studentId;

    private Long subjectId;

    /** 1 简单 / 2 中等 / 3 困难 */
    private Integer difficulty;

    private Integer questionCount;

    /** 综合分 0.0-10.0 */
    private BigDecimal overallScore;

    private Integer totalDuration;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}

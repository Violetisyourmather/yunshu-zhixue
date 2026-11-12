package com.yunshu.zhixue.modules.student.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学员面试记录查询行（interview_record + subject）。
 */
@Data
public class StudentInterviewRow {

    private Long id;

    private Long subjectId;

    private String subjectName;

    private Integer difficulty;

    private Integer questionCount;

    private BigDecimal overallScore;

    private Integer totalDuration;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}

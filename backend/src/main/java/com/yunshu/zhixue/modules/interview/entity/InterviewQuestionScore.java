package com.yunshu.zhixue.modules.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 面试逐题评分（03 interview_question_score）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interview_question_score")
public class InterviewQuestionScore extends BaseEntity {

    private Long interviewRecordId;

    private Integer questionOrder;

    private Long questionId;

    private String questionText;

    private String studentAnswer;

    private BigDecimal score;

    private String aiFeedback;
}

package com.yunshu.zhixue.modules.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 面试报告（03 interview_report），JSON 字段由应用层序列化。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interview_report")
public class InterviewReport extends BaseEntity {

    private Long interviewRecordId;

    private String radarJson;

    private String strengths;

    private String weaknesses;

    private String suggestions;

    private String improvementCardsJson;
}

package com.yunshu.zhixue.modules.interviewbank.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 面试题主题词表（03 §4.2 interview_topic）。
 *
 * <p>{@code subjectId} 可为空（全局主题）；题目表使用主题名称字符串关联。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interview_topic")
public class InterviewTopic extends BaseEntity {

    private Long subjectId;

    private String name;

    private Integer sortOrder;
}

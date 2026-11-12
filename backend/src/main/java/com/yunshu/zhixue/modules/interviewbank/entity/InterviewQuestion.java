package com.yunshu.zhixue.modules.interviewbank.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 面试题库题目（03 §4.2 interview_question）。
 *
 * <p>方向 = 学科；主题存字符串（与 interview_topic 词表对应），NULL 表示未分类。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interview_question")
public class InterviewQuestion extends BaseEntity {

    /** 方向 → subject.id */
    private Long subjectId;

    private String content;

    private String referenceAnswer;

    /** 主题；NULL = 未分类 */
    private String topic;

    /** 1 简单 / 2 中等 / 3 困难 */
    private Integer difficulty;

    private String tags;

    /** 1 文档解析 / 2 手工录入 / 3 手动导入 */
    private Integer sourceType;

    private String sourceDocName;

    private Integer sourceSeq;

    /** 1 已上架 / 2 已下架 */
    private Integer status;
}

package com.yunshu.zhixue.modules.question.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 题目选项（03 §4.2 question_option）。
 *
 * <p>选项为题目子数据，编辑时物理删除后重建，避免逻辑删除行占用
 * {@code uk_question_option(question_id, option_key)} 唯一键。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_option")
public class QuestionOption extends BaseEntity {

    /** → question.id */
    private Long questionId;

    /** A / B / C ... */
    private String optionKey;

    private String optionText;

    /** 1 正确 / 0 否 */
    private Integer isCorrect;

    private Integer sortOrder;
}

package com.yunshu.zhixue.modules.question.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 题库题目（03 §4.2 question）。
 *
 * <p>题目挂在阶段下；选择题的正确项存 {@code question_option.is_correct}，
 * 判断题与简答题的答案存 {@code answer}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question")
public class Question extends BaseEntity {

    /** → stage.id */
    private Long stageId;

    /** 1 单选 / 2 多选 / 3 判断 / 4 简答 */
    private Integer type;

    /** 1 简单 / 2 中等 / 3 困难 */
    private Integer difficulty;

    private String content;

    /** 判断题答案 / 简答参考答案；选择题为空。 */
    private String answer;

    private String analysis;

    /** 标签，逗号分隔。 */
    private String tags;

    /** 1 已上架 / 2 已下架 / 3 待审核 / 4 草稿 / 5 已驳回 */
    private Integer status;

    private String rejectReason;
}

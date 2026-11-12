package com.yunshu.zhixue.modules.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 错题本记录（03 wrong_question）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wrong_question")
public class WrongQuestion extends BaseEntity {

    private Long studentId;

    private Long questionId;

    private Integer errorCount;

    private Integer correctedCount;

    /** 1 待复习 / 2 已掌握 */
    private Integer status;

    private LocalDateTime lastWrongAt;

    /** 最近一次错误作答快照（客观题为选项标识，简答为文本）。 */
    private String lastAnswer;
}

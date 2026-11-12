package com.yunshu.zhixue.modules.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 刷题作答明细（03 practice_record）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("practice_record")
public class PracticeRecord extends BaseEntity {

    private Long studentId;

    private Long questionId;

    /** 1 顺序 / 2 随机 */
    private Integer mode;

    /** 1 对 / 0 错 */
    private Integer isCorrect;

    private Integer durationSeconds;

    /** 作答时间（UTC）。 */
    private LocalDateTime practicedAt;
}

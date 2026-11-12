package com.yunshu.zhixue.modules.subject.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 阶段（03 §4.2 stage）：学科下的第二层，题目挂载到阶段。
 *
 * <p>同一学科内阶段名唯一（uk_stage_subject_name）；停用阶段后其下题目不在学员端展示。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stage")
public class Stage extends BaseEntity {

    /** → subject.id */
    private Long subjectId;

    private String name;

    /** 图标（Emoji），留空时前端展示默认图标。 */
    private String icon;

    private Integer sortOrder;

    /** 1 启用 / 0 停用 */
    private Integer status;
}

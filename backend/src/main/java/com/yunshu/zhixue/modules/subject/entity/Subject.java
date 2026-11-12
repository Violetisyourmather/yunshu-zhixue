package com.yunshu.zhixue.modules.subject.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学科（03 §4.2 subject），内容体系根节点。
 *
 * <p>面试方向直接复用学科，不另建映射表；课程、学习路线通过 {@code subject_id} 逻辑关联。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("subject")
public class Subject extends BaseEntity {

    private String name;

    /** 图标（Emoji，必填）。 */
    private String icon;

    private String description;

    /** 主题色，形如 {@code #0F6B57}。 */
    private String themeColor;

    /** 排序：越小越靠前。 */
    private Integer sortOrder;
}

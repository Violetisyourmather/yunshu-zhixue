package com.yunshu.zhixue.modules.system.entity;

import com.yunshu.zhixue.common.persistence.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限点（03 §4.5 permission）：{@code type=1} 为 9 个分组，{@code type=2} 为 35 个操作点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("permission")
public class Permission extends BaseEntity {

    /** 父节点；NULL = 组节点 */
    private Long parentId;

    private String name;

    private String code;

    /** 1 组 / 2 操作点 */
    private Integer type;

    private Integer sortOrder;
}

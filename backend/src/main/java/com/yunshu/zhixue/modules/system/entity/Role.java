package com.yunshu.zhixue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台角色（03 §4.5 role）。超级管理员角色编码固定为 {@code super_admin}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("role")
public class Role extends BaseEntity {

    private String name;

    private String code;

    private String description;
}

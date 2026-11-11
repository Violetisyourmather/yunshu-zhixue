package com.yunshu.zhixue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 后台用户（03 §4.5 admin_user）。
 *
 * <p>{@code username} 与 {@code mobile} 均有唯一键，登录时先按用户名命中、再按手机号命中。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_user")
public class AdminUser extends BaseEntity {

    private String username;

    /** BCrypt 密码哈希，任何接口都不得返回。 */
    private String passwordHash;

    private String mobile;

    private String email;

    /** → role.id */
    private Long roleId;

    /** 1 已启用 / 0 已禁用 */
    private Integer status;

    /** 1 内置超级管理员（角色不可变更、不可删除） */
    @TableField("is_builtin")
    private Integer builtin;

    private LocalDateTime lastLoginAt;

    /** 账号是否可用于登录。 */
    public boolean enabled() {
        return status != null && status == 1;
    }
}

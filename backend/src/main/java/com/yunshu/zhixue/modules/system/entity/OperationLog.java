package com.yunshu.zhixue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志（03 §4.5 operation_log）。
 *
 * <p>审计表例外：无逻辑删除、无更新时间，清空为物理删除（03 §6）。</p>
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** → admin_user.id */
    private Long userId;

    /** 用户名快照（账号后续改名/删除后仍保留当时值）。 */
    private String username;

    /** 登录/登出/新增/修改/删除/禁用启用/导出/其他 */
    private String action;

    /** 对象类型，如「后台用户」「角色」「系统设置」。 */
    private String targetType;

    /** 对象描述，如用户名、角色名。 */
    private String targetName;

    private String ip;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

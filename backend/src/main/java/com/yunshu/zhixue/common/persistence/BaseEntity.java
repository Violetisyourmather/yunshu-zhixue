package com.yunshu.zhixue.common.persistence;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务表公共字段（database-rules：自增主键、UTC 时间自动维护、逻辑删除）。
 *
 * <p>例外表不继承本类：{@code operation_log}（审计，无逻辑删除）、
 * {@code role_permission}（覆盖式授权，无逻辑删除）——见 03 §6。</p>
 */
@Data
public abstract class BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建时间（UTC），插入时自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（UTC），插入与更新时自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记：0 未删除 / 1 已删除。 */
    @TableLogic
    @TableField("delete_flag")
    private Integer deleteFlag;
}

package com.yunshu.zhixue.modules.student.entity;

import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 学员（03 §4.1 student）。
 *
 * <p>登录账号为 {@code mobile} 或 {@code email}；两者在库中均有唯一约束。
 * {@code nickname} 无唯一约束，因此不参与登录。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("student")
public class Student extends BaseEntity {

    /** 手机号（登录账号，唯一） */
    private String mobile;

    /** BCrypt 密码哈希，任何接口都不得返回。 */
    private String passwordHash;

    private String nickname;

    private String avatar;

    /** 邮箱（可空，非空值唯一） */
    private String email;

    /** 0 保密 / 1 男 / 2 女 */
    private Integer gender;

    /** → subject.id */
    private Long preferredSubjectId;

    /** 新手引导是否看过：0/1 */
    private Integer guideFinished;

    /** 1 已启用 / 0 已禁用 */
    private Integer status;

    private LocalDateTime lastLoginAt;

    /** 账号是否可用于登录。 */
    public boolean enabled() {
        return status != null && status == 1;
    }
}

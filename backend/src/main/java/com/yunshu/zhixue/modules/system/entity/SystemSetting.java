package com.yunshu.zhixue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统设置（03 §4.5 system_setting，key-value）。
 *
 * <p>仅基本信息：site_name / site_description / logo_url / default_avatar。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_setting")
public class SystemSetting extends BaseEntity {

    private String configKey;

    private String configValue;

    private String remark;
}

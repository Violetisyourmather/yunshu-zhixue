package com.yunshu.zhixue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.system.entity.SystemSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统设置持久层（key-value，按 config_key 唯一）。
 */
@Mapper
public interface SystemSettingMapper extends BaseMapper<SystemSetting> {
}

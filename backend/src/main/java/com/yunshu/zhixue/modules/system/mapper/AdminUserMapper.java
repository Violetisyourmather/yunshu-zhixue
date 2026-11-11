package com.yunshu.zhixue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台用户持久层（MyBatis-Plus，逻辑删除由全局配置处理）。
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {
}

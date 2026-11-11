package com.yunshu.zhixue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.system.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台角色持久层。
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}

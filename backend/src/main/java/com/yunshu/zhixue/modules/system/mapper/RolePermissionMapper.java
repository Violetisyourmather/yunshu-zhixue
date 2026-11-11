package com.yunshu.zhixue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.system.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色-权限关联持久层（物理表，无逻辑删除）。
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}

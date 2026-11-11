package com.yunshu.zhixue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.system.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限点查询（role_permission 为覆盖式授权，无逻辑删除；见 03 §6）。
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /** 角色拥有的操作点权限码（不含组节点），按排序返回。 */
    @Select("""
            SELECT p.code
            FROM permission p
            INNER JOIN role_permission rp ON rp.permission_id = p.id
            WHERE rp.role_id = #{roleId}
              AND p.type = 2
              AND p.delete_flag = 0
            ORDER BY p.sort_order, p.id
            """)
    List<String> selectCodesByRoleId(@Param("roleId") long roleId);
}

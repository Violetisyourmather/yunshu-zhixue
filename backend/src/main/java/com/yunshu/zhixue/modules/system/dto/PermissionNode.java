package com.yunshu.zhixue.modules.system.dto;

import java.util.List;

/**
 * A-L04 权限树节点：组节点 {@code type=1}（children 为操作点），操作点 {@code type=2}（children 为空）。
 *
 * @param checked 角色是否已勾选；组节点仅在全部子项勾选时为 true
 */
public record PermissionNode(
        Long id,
        String name,
        String code,
        Integer type,
        boolean checked,
        List<PermissionNode> children) {
}

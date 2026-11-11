package com.yunshu.zhixue.modules.system.dto;

import java.util.List;

/**
 * A-L05 保存角色权限（全量覆盖）。
 *
 * <p>前端树提交的 id 可包含组节点，服务端仅落库 {@code type=2} 的操作点。</p>
 */
public record RolePermissionSaveRequest(List<Long> permissionIds) {
}

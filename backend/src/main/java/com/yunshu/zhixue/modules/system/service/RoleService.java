package com.yunshu.zhixue.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.system.dto.PermissionNode;
import com.yunshu.zhixue.modules.system.dto.RoleCard;
import com.yunshu.zhixue.modules.system.dto.RoleCreateRequest;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.entity.Permission;
import com.yunshu.zhixue.modules.system.entity.Role;
import com.yunshu.zhixue.modules.system.entity.RolePermission;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.mapper.PermissionMapper;
import com.yunshu.zhixue.modules.system.mapper.RoleMapper;
import com.yunshu.zhixue.modules.system.mapper.RolePermissionMapper;
import com.yunshu.zhixue.security.SecurityUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 角色权限（A-L01 ~ A-L05）。
 *
 * <p>不设权限点，仅超级管理员可访问（D4-5）；每次调用先校验当前登录人的角色编码。
 * 权限保存为全量覆盖：先物理删除 role_permission，再批量插入，同一事务。</p>
 */
@Service
public class RoleService {

    private static final int TYPE_GROUP = 1;
    private static final int TYPE_OPERATION = 2;
    /** 运营人员与内容审核角色不可见的权限组：用户管理与系统管理（需求 4.5.2）。 */
    private static final Set<String> PRESET_HIDDEN_GROUP_CODES =
            Set.of("group:user", "group:system");

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final AdminUserMapper adminUserMapper;
    private final OperationLogRecorder operationLogRecorder;

    public RoleService(RoleMapper roleMapper,
                       PermissionMapper permissionMapper,
                       RolePermissionMapper rolePermissionMapper,
                       AdminUserMapper adminUserMapper,
                       OperationLogRecorder operationLogRecorder) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.adminUserMapper = adminUserMapper;
        this.operationLogRecorder = operationLogRecorder;
    }

    /** A-L01 角色卡片：用户数 + 权限标签。 */
    public List<RoleCard> list() {
        requireSuperAdmin();
        List<Role> roles = roleMapper.selectList(Wrappers.<Role>lambdaQuery().orderByAsc(Role::getId));
        List<Permission> permissions = allPermissions();
        List<Permission> operations = permissions.stream()
                .filter(p -> Objects.equals(p.getType(), TYPE_OPERATION))
                .toList();
        Map<Long, Permission> byId = permissions.stream()
                .collect(Collectors.toMap(Permission::getId, Function.identity(), (a, b) -> a));
        Map<Long, Long> userCounts = adminUserMapper.selectList(null).stream()
                .map(AdminUser::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Map<Long, Set<Long>> granted = loadGrantedMap();

        return roles.stream().map(role -> {
            boolean builtin = AppConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode());
            Set<Long> ids = builtin
                    ? operations.stream().map(Permission::getId).collect(Collectors.toSet())
                    : granted.getOrDefault(role.getId(), Set.of());
            List<String> labels = new ArrayList<>();
            for (Permission operation : operations) {
                if (!ids.contains(operation.getId())) {
                    continue;
                }
                Permission group = byId.get(operation.getParentId());
                labels.add(group == null ? operation.getName() : group.getName() + "·" + operation.getName());
            }
            return new RoleCard(role.getId(), role.getName(), role.getCode(), role.getDescription(),
                    userCounts.getOrDefault(role.getId(), 0L), labels.size(), labels, builtin);
        }).toList();
    }

    /** A-L02 新增角色（名称、编码均唯一）。 */
    @Transactional
    public Long create(RoleCreateRequest request) {
        requireSuperAdmin();
        String name = request.name().trim();
        String code = request.code().trim();
        if (roleMapper.selectCount(Wrappers.<Role>lambdaQuery().eq(Role::getCode, code)) > 0) {
            throw new BizException(ErrorCode.NAME_EXISTS, "角色编码已存在");
        }
        if (roleMapper.selectCount(Wrappers.<Role>lambdaQuery().eq(Role::getName, name)) > 0) {
            throw new BizException(ErrorCode.NAME_EXISTS, "角色名称已存在");
        }

        Role role = new Role();
        role.setName(name);
        role.setCode(code);
        role.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        roleMapper.insert(role);
        operationLogRecorder.record(OperationActions.CREATE, "角色", name);
        return role.getId();
    }

    /** A-L03 删除角色：有用户 → 30003；超管角色 → 30004。 */
    @Transactional
    public void delete(Long id) {
        requireSuperAdmin();
        Role role = requireRole(id);
        if (AppConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode())) {
            throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED, "超级管理员角色不可删除");
        }
        long userCount = adminUserMapper.selectCount(
                Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getRoleId, id));
        if (userCount > 0) {
            throw new BizException(ErrorCode.ROLE_IN_USE, "该角色下存在 " + userCount + " 个用户，不可删除");
        }
        rolePermissionMapper.delete(Wrappers.<RolePermission>lambdaQuery()
                .eq(RolePermission::getRoleId, id));
        roleMapper.deleteById(id);
        operationLogRecorder.record(OperationActions.DELETE, "角色", role.getName());
    }

    /** A-L04 权限树：9 组 35 项，附指定角色的勾选状态。 */
    public List<PermissionNode> permissionTree(Long roleId) {
        requireSuperAdmin();
        Role role = roleId == null ? null : requireRole(roleId);
        Set<Long> checkedIds;
        if (role != null && AppConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode())) {
            checkedIds = allPermissions().stream()
                    .filter(p -> Objects.equals(p.getType(), TYPE_OPERATION))
                    .map(Permission::getId)
                    .collect(Collectors.toSet());
        } else if (role != null) {
            checkedIds = rolePermissionMapper.selectList(Wrappers.<RolePermission>lambdaQuery()
                            .eq(RolePermission::getRoleId, roleId)).stream()
                    .map(RolePermission::getPermissionId)
                    .collect(Collectors.toSet());
        } else {
            checkedIds = Set.of();
        }
        Set<String> hiddenGroups = role != null && isPresetRestrictedRole(role)
                ? PRESET_HIDDEN_GROUP_CODES : Set.of();
        return buildTree(allPermissions(), checkedIds, hiddenGroups);
    }

    /** A-L05 保存角色权限（全量覆盖，单事务）。 */
    @Transactional
    public void savePermissions(Long roleId, List<Long> permissionIds) {
        requireSuperAdmin();
        Role role = requireRole(roleId);
        if (AppConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode())) {
            throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED, "超级管理员角色权限不可修改");
        }

        Set<Long> requested = permissionIds == null ? Set.of() : new HashSet<>(permissionIds);
        List<Long> selected = permissionMapper.selectList(Wrappers.<Permission>lambdaQuery()
                        .eq(Permission::getType, TYPE_OPERATION)).stream()
                .filter(p -> requested.contains(p.getId()))
                .map(Permission::getId)
                .toList();

        if (isPresetRestrictedRole(role)) {
            Set<Long> blocked = presetHiddenPermissionIds();
            if (selected.stream().anyMatch(blocked::contains)) {
                throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED,
                        role.getName() + "不可授予用户管理或系统管理权限");
            }
        }

        try {
            rolePermissionMapper.delete(Wrappers.<RolePermission>lambdaQuery()
                    .eq(RolePermission::getRoleId, roleId));
            for (Long permissionId : selected) {
                RolePermission mapping = new RolePermission();
                mapping.setRoleId(roleId);
                mapping.setPermissionId(permissionId);
                rolePermissionMapper.insert(mapping);
            }
        } catch (DataAccessException ex) {
            throw new BizException(ErrorCode.PERMISSION_SAVE_FAILED);
        }
        operationLogRecorder.record(OperationActions.UPDATE, "角色", "权限配置：" + role.getName());
    }

    /** 预置业务角色（运营人员、内容审核）不展示也不可被授予用户管理/系统管理。 */
    private boolean isPresetRestrictedRole(Role role) {
        return AppConstants.OPERATOR_ROLE_CODE.equals(role.getCode())
                || AppConstants.CONTENT_REVIEWER_ROLE_CODE.equals(role.getCode());
    }

    /** 用户管理 + 系统管理两组下的全部操作权限点，预置业务角色不可被授予。 */
    private Set<Long> presetHiddenPermissionIds() {
        List<Permission> all = allPermissions();
        Set<Long> hiddenGroupIds = all.stream()
                .filter(p -> Objects.equals(p.getType(), TYPE_GROUP))
                .filter(p -> PRESET_HIDDEN_GROUP_CODES.contains(p.getCode()))
                .map(Permission::getId)
                .collect(Collectors.toSet());
        return all.stream()
                .filter(p -> Objects.equals(p.getType(), TYPE_OPERATION))
                .filter(p -> hiddenGroupIds.contains(p.getParentId()))
                .map(Permission::getId)
                .collect(Collectors.toSet());
    }

    private List<PermissionNode> buildTree(List<Permission> permissions, Set<Long> checkedIds,
                                           Set<String> hiddenGroupCodes) {
        Map<Long, List<Permission>> childrenByParent = permissions.stream()
                .filter(p -> p.getParentId() != null)
                .collect(Collectors.groupingBy(Permission::getParentId, LinkedHashMap::new, Collectors.toList()));

        return permissions.stream()
                .filter(p -> Objects.equals(p.getType(), TYPE_GROUP))
                .filter(group -> !hiddenGroupCodes.contains(group.getCode()))
                .map(group -> {
                    List<PermissionNode> children = childrenByParent
                            .getOrDefault(group.getId(), List.of()).stream()
                            .map(child -> new PermissionNode(child.getId(), child.getName(), child.getCode(),
                                    child.getType(), checkedIds.contains(child.getId()), List.of()))
                            .toList();
                    boolean checked = !children.isEmpty() && children.stream().allMatch(PermissionNode::checked);
                    return new PermissionNode(group.getId(), group.getName(), group.getCode(),
                            group.getType(), checked, children);
                })
                .toList();
    }

    private Map<Long, Set<Long>> loadGrantedMap() {
        Map<Long, Set<Long>> result = new LinkedHashMap<>();
        for (RolePermission mapping : rolePermissionMapper.selectList(null)) {
            result.computeIfAbsent(mapping.getRoleId(), key -> new HashSet<>())
                    .add(mapping.getPermissionId());
        }
        return result;
    }

    private List<Permission> allPermissions() {
        return permissionMapper.selectList(Wrappers.<Permission>lambdaQuery()
                .orderByAsc(Permission::getSortOrder)
                .orderByAsc(Permission::getId));
    }

    private Role requireRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    /** 角色管理无权限点，仅 super_admin 角色可访问（04 §2.8）。 */
    private void requireSuperAdmin() {
        AdminUser current = adminUserMapper.selectById(SecurityUtils.currentUserId());
        if (current == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        Role role = current.getRoleId() == null ? null : roleMapper.selectById(current.getRoleId());
        if (role == null || !AppConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }
}

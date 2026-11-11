package com.yunshu.zhixue.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.system.dto.BatchStatusRequest;
import com.yunshu.zhixue.modules.system.dto.RecentOperation;
import com.yunshu.zhixue.modules.system.dto.ResetPasswordResponse;
import com.yunshu.zhixue.modules.system.dto.RoleOption;
import com.yunshu.zhixue.modules.system.dto.UserCreateRequest;
import com.yunshu.zhixue.modules.system.dto.UserDetail;
import com.yunshu.zhixue.modules.system.dto.UserItem;
import com.yunshu.zhixue.modules.system.dto.UserUpdateRequest;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.entity.OperationLog;
import com.yunshu.zhixue.modules.system.entity.Role;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.mapper.OperationLogMapper;
import com.yunshu.zhixue.modules.system.mapper.RoleMapper;
import com.yunshu.zhixue.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理（A-M01 ~ A-M08）。
 *
 * <p>约束（需求 4.5.1 / 03 §4.5）：不允许新增或切换为超级管理员；
 * 内置超级管理员账号不可删除、不可禁用、角色只读。</p>
 */
@Service
public class AdminUserService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int RECENT_LOG_LIMIT = 10;

    private final AdminUserMapper adminUserMapper;
    private final RoleMapper roleMapper;
    private final OperationLogMapper operationLogMapper;
    private final OperationLogRecorder operationLogRecorder;
    private final PasswordEncoder passwordEncoder;
    private final String defaultPassword;

    public AdminUserService(AdminUserMapper adminUserMapper,
                            RoleMapper roleMapper,
                            OperationLogMapper operationLogMapper,
                            OperationLogRecorder operationLogRecorder,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.default-password}") String defaultPassword) {
        this.adminUserMapper = adminUserMapper;
        this.roleMapper = roleMapper;
        this.operationLogMapper = operationLogMapper;
        this.operationLogRecorder = operationLogRecorder;
        this.passwordEncoder = passwordEncoder;
        this.defaultPassword = defaultPassword;
    }

    /** A-M01 列表：用户名/手机号关键词 + 角色 + 状态筛选。 */
    public PageResult<UserItem> list(String keyword, Long roleId, Integer status, long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        var query = Wrappers.<AdminUser>lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(w -> w.like(AdminUser::getUsername, value).or().like(AdminUser::getMobile, value));
        }
        if (roleId != null) {
            query.eq(AdminUser::getRoleId, roleId);
        }
        if (status != null) {
            query.eq(AdminUser::getStatus, status);
        }
        query.orderByAsc(AdminUser::getId);

        Page<AdminUser> result = adminUserMapper.selectPage(new Page<>(safePage, safePageSize), query);
        Map<Long, Role> roles = loadRoles(result.getRecords());
        List<UserItem> items = result.getRecords().stream()
                .map(user -> toItem(user, roles.get(user.getRoleId())))
                .toList();
        return PageResult.of(items, result.getTotal(), safePage, safePageSize);
    }

    /** 用户管理页角色下拉（含超级管理员，前端在新增/编辑表单中过滤）。 */
    public List<RoleOption> roleOptions() {
        return roleMapper.selectList(Wrappers.<Role>lambdaQuery().orderByAsc(Role::getId)).stream()
                .map(role -> new RoleOption(role.getId(), role.getName(), role.getCode()))
                .toList();
    }

    /** A-M02 新增用户。 */
    @Transactional
    public UserItem create(UserCreateRequest request) {
        String username = request.username().trim();
        String mobile = normalize(request.mobile());
        String email = normalize(request.email());
        Role role = requireAssignableRole(request.roleId());
        ensureUnique(username, mobile, null);

        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setMobile(mobile);
        user.setEmail(email);
        user.setRoleId(role.getId());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(requireStatus(request.status(), 1));
        user.setBuiltin(0);
        try {
            adminUserMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.NAME_EXISTS, "用户名或手机号已存在");
        }

        operationLogRecorder.record(OperationActions.CREATE, "后台用户", username);
        return toItem(adminUserMapper.selectById(user.getId()), role);
    }

    /** A-M03 编辑用户：密码留空不修改；内置账号角色只读。 */
    @Transactional
    public void update(Long id, UserUpdateRequest request) {
        AdminUser existing = requireUser(id);
        requireCanMaintainSuperAdmin(existing);
        String username = request.username().trim();
        String mobile = normalize(request.mobile());
        String email = normalize(request.email());

        if (mobile == null && !isBuiltin(existing)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "手机号不能为空");
        }

        boolean roleChanged = !Objects.equals(existing.getRoleId(), request.roleId());
        if (Boolean.TRUE.equals(isBuiltin(existing)) && roleChanged) {
            throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED, "内置超级管理员账号的角色不可修改");
        }
        if (roleChanged) {
            // RBAC 分级：角色调整属于授权操作，仅超级管理员可执行；任何角色都不可调整自己的角色
            if (isCurrentUser(id)) {
                throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED, "不能修改当前登录账号的角色");
            }
            if (!isCurrentSuperAdmin()) {
                throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED, "仅超级管理员可调整用户角色");
            }
            requireAssignableRole(request.roleId());
        }

        int status = request.status() == null ? existing.getStatus() : requireStatus(request.status(), 1);
        if (status != 1 && (isBuiltin(existing) || isCurrentUser(id))) {
            throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED,
                    "内置账号、超管角色或当前登录账号不可禁用");
        }
        ensureUnique(username, mobile, id);

        AdminUser update = new AdminUser();
        update.setId(id);
        update.setUsername(username);
        update.setMobile(mobile);
        update.setEmail(email);
        update.setRoleId(request.roleId());
        update.setStatus(status);
        if (StringUtils.hasText(request.password())) {
            if (request.password().length() < 6 || request.password().length() > 20) {
                throw new BizException(ErrorCode.PARAM_INVALID, "密码长度需为 6-20 位");
            }
            update.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        try {
            adminUserMapper.updateById(update);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.NAME_EXISTS, "用户名或手机号已存在");
        }

        String action = !Objects.equals(existing.getStatus(), status)
                ? OperationActions.STATUS : OperationActions.UPDATE;
        operationLogRecorder.record(action, "后台用户", username);
    }

    /** A-M04 批量启用 / 禁用；内置账号不可禁用。 */
    @Transactional
    public void batchStatus(BatchStatusRequest request) {
        int status = requireStatus(request.status(), 1);
        List<AdminUser> users = adminUserMapper.selectBatchIds(request.ids());
        if (users.isEmpty()) {
            return;
        }
        if (status != 1 && users.stream().anyMatch(user -> isBuiltin(user) || isCurrentUser(user.getId()))) {
            throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED,
                    "内置账号、超管角色或当前登录账号不可禁用");
        }
        adminUserMapper.update(null, Wrappers.<AdminUser>lambdaUpdate()
                .in(AdminUser::getId, request.ids())
                .set(AdminUser::getStatus, status)
                .set(AdminUser::getUpdatedAt, LocalDateTime.now(ZoneOffset.UTC)));
        operationLogRecorder.record(OperationActions.STATUS, "后台用户",
                (status == 1 ? "批量启用 " : "批量禁用 ") + users.size() + " 个用户");
    }

    /** A-M05 删除用户（逻辑删除）；内置账号不可删除。 */
    @Transactional
    public void delete(Long id) {
        AdminUser user = requireUser(id);
        if (isBuiltin(user) || isCurrentUser(id)) {
            throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED,
                    "内置账号、超管角色或当前登录账号不可删除");
        }
        releaseUniqueFields(user);
        adminUserMapper.deleteById(id);
        operationLogRecorder.record(OperationActions.DELETE, "后台用户", user.getUsername());
    }

    /** A-M06 批量删除。 */
    @Transactional
    public void batchDelete(Collection<Long> ids) {
        List<AdminUser> users = adminUserMapper.selectBatchIds(ids);
        if (users.isEmpty()) {
            return;
        }
        if (users.stream().anyMatch(user -> isBuiltin(user) || isCurrentUser(user.getId()))) {
            throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED,
                    "内置账号、超管角色或当前登录账号不可删除");
        }
        users.forEach(this::releaseUniqueFields);
        adminUserMapper.deleteBatchIds(ids);
        operationLogRecorder.record(OperationActions.DELETE, "后台用户", "批量删除 " + users.size() + " 个用户");
    }

    /**
     * 逻辑删除前释放唯一值（03 §4.4 / R4）：手机号置 NULL、用户名追加 {@code #del{id}}。
     *
     * <p>逻辑删除行仍保留在表中，若不释放唯一键，已删除账号的用户名/手机号
     * 将无法被新账号重新使用。用户名按 50 字符上限截断原值以保留完整后缀。</p>
     */
    private void releaseUniqueFields(AdminUser user) {
        String suffix = "#del" + user.getId();
        String username = user.getUsername();
        int keep = Math.max(0, 50 - suffix.length());
        String released = (username.length() > keep ? username.substring(0, keep) : username) + suffix;
        adminUserMapper.update(null, Wrappers.<AdminUser>lambdaUpdate()
                .eq(AdminUser::getId, user.getId())
                .set(AdminUser::getUsername, released)
                .set(AdminUser::getMobile, null));
    }

    /** A-M07 重置为部署期默认密码，明文只在本次响应返回。 */
    @Transactional
    public ResetPasswordResponse resetPassword(Long id) {
        AdminUser user = requireUser(id);
        requireCanMaintainSuperAdmin(user);
        AdminUser update = new AdminUser();
        update.setId(id);
        update.setPasswordHash(passwordEncoder.encode(defaultPassword));
        adminUserMapper.updateById(update);
        operationLogRecorder.record(OperationActions.UPDATE, "后台用户", "重置密码：" + user.getUsername());
        return new ResetPasswordResponse(defaultPassword);
    }

    /** A-M08 详情 + 最近 10 条操作。 */
    public UserDetail detail(Long id) {
        AdminUser user = requireUser(id);
        requireCanViewSuperAdminDetail(user);
        Role role = user.getRoleId() == null ? null : roleMapper.selectById(user.getRoleId());
        List<OperationLog> logs = operationLogMapper.selectList(Wrappers.<OperationLog>lambdaQuery()
                .eq(OperationLog::getUserId, id)
                .orderByDesc(OperationLog::getId)
                .last("LIMIT " + RECENT_LOG_LIMIT));
        List<RecentOperation> recent = logs.stream()
                .map(entry -> new RecentOperation(entry.getId(), entry.getAction(), entry.getTargetType(),
                        entry.getTargetName(), entry.getIp(), entry.getCreatedAt()))
                .toList();
        return new UserDetail(user.getId(), user.getUsername(), user.getMobile(), user.getEmail(),
                user.getRoleId(), role == null ? null : role.getName(), role == null ? null : role.getCode(),
                user.getStatus(), isBuiltin(user), user.getCreatedAt(), user.getLastLoginAt(), recent);
    }

    private AdminUser requireUser(Long id) {
        AdminUser user = adminUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private Role requireAssignableRole(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "角色不存在");
        }
        if (AppConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode())) {
            throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED, "不允许创建或切换为超级管理员");
        }
        return role;
    }

    private void ensureUnique(String username, String mobile, Long excludeId) {
        var query = Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getUsername, username);
        if (excludeId != null) {
            query.ne(AdminUser::getId, excludeId);
        }
        if (adminUserMapper.selectCount(query) > 0) {
            throw new BizException(ErrorCode.NAME_EXISTS, "用户名已存在");
        }
        if (mobile != null) {
            var mobileQuery = Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getMobile, mobile);
            if (excludeId != null) {
                mobileQuery.ne(AdminUser::getId, excludeId);
            }
            if (adminUserMapper.selectCount(mobileQuery) > 0) {
                throw new BizException(ErrorCode.MOBILE_EXISTS, "手机号已存在");
            }
        }
    }

    private Map<Long, Role> loadRoles(List<AdminUser> users) {
        Set<Long> roleIds = users.stream().map(AdminUser::getRoleId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(Role::getId, role -> role, (a, b) -> a));
    }

    private UserItem toItem(AdminUser user, Role role) {
        return new UserItem(user.getId(), user.getUsername(), user.getMobile(), user.getEmail(),
                user.getRoleId(), role == null ? null : role.getName(),
                role == null ? null : role.getCode(), user.getStatus(), isBuiltin(user),
                user.getLastLoginAt(), user.getCreatedAt());
    }

    private static boolean isBuiltin(AdminUser user) {
        return Integer.valueOf(1).equals(user.getBuiltin());
    }

    /** 需求未定义自操作：禁止管理员禁用/删除自己，避免自锁。 */
    private static boolean isCurrentUser(Long id) {
        return id != null && id == SecurityUtils.currentUserId();
    }

    /**
     * 超级管理员账号仅超级管理员可维护。
     *
     * <p>运营等角色虽拥有 user:edit，但若能编辑超管资料或重置其密码，
     * 即可改绑账号或接管超管，因此编辑与重置密码必须单独做角色校验。</p>
     */
    private void requireCanMaintainSuperAdmin(AdminUser target) {
        if (!isSuperAdminRole(target.getRoleId())) {
            return;
        }
        if (isCurrentSuperAdmin()) {
            return;
        }
        throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED, "超级管理员账号仅超级管理员可操作");
    }

    /** 非超级管理员不可查看超级管理员详情（含手机号、邮箱与最近操作记录）。 */
    private void requireCanViewSuperAdminDetail(AdminUser target) {
        if (isSuperAdminRole(target.getRoleId()) && !isCurrentSuperAdmin()) {
            throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED, "超级管理员账号仅超级管理员可查看");
        }
    }

    private boolean isCurrentSuperAdmin() {
        AdminUser current = adminUserMapper.selectById(SecurityUtils.currentUserId());
        return current != null && isSuperAdminRole(current.getRoleId());
    }

    private boolean isSuperAdminRole(Long roleId) {
        if (roleId == null) {
            return false;
        }
        Role role = roleMapper.selectById(roleId);
        return role != null && AppConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode());
    }

    private static int requireStatus(Integer status, int defaultValue) {
        if (status == null) {
            return defaultValue;
        }
        if (status != 0 && status != 1) {
            throw new BizException(ErrorCode.PARAM_INVALID, "状态值不合法");
        }
        return status;
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

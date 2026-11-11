package com.yunshu.zhixue.modules.system;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 内置管理员密码初始化（03 §4.5 种子说明、需求 4.5.1）。
 *
 * <p>schema.sql 中的种子密码是占位哈希、禁止直接登录；首次启动时若检测到占位值，
 * 用 {@code app.default-password} 生成 BCrypt 哈希写入，之后启动不再覆盖。</p>
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);
    private static final String BUILTIN_USERNAME = "admin";
    private static final String PLACEHOLDER_HASH_PREFIX = "$2a$10$000000";

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final String defaultPassword;

    public AdminAccountInitializer(AdminUserMapper adminUserMapper,
                                   PasswordEncoder passwordEncoder,
                                   @Value("${app.default-password:}") String defaultPassword) {
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.defaultPassword = defaultPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        AdminUser admin = adminUserMapper.selectOne(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getUsername, BUILTIN_USERNAME)
                .eq(AdminUser::getBuiltin, 1));
        if (admin == null || !isPlaceholder(admin.getPasswordHash())) {
            return;
        }
        if (defaultPassword == null || defaultPassword.isBlank()) {
            log.warn("内置管理员仍为占位密码哈希，且未配置 app.default-password，管理员将无法登录");
            return;
        }

        AdminUser update = new AdminUser();
        update.setId(admin.getId());
        update.setPasswordHash(passwordEncoder.encode(defaultPassword));
        adminUserMapper.updateById(update);
        log.warn("已为内置管理员 {} 生成默认密码哈希，请登录后立即修改密码", BUILTIN_USERNAME);
    }

    private boolean isPlaceholder(String passwordHash) {
        return passwordHash != null && passwordHash.startsWith(PLACEHOLDER_HASH_PREFIX);
    }
}

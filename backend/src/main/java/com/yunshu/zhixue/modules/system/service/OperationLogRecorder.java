package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.config.AsyncConfig;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.entity.OperationLog;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.mapper.OperationLogMapper;
import com.yunshu.zhixue.security.Audience;
import com.yunshu.zhixue.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志记录器（02 §2.5：异步落库，不引入 MQ）。
 *
 * <p>用户名与 IP 在调用线程内同步解析，再提交线程池插入，避免异步线程丢失请求上下文；
 * 日志写入失败只告警，不影响主流程。</p>
 */
@Component
public class OperationLogRecorder {

    private static final Logger log = LoggerFactory.getLogger(OperationLogRecorder.class);

    private static final int IP_MAX_LENGTH = 64;

    private final OperationLogMapper operationLogMapper;
    private final AdminUserMapper adminUserMapper;
    private final ThreadPoolTaskExecutor executor;

    public OperationLogRecorder(OperationLogMapper operationLogMapper,
                                AdminUserMapper adminUserMapper,
                                @Qualifier(AsyncConfig.OPERATION_LOG_EXECUTOR) ThreadPoolTaskExecutor executor) {
        this.operationLogMapper = operationLogMapper;
        this.adminUserMapper = adminUserMapper;
        this.executor = executor;
    }

    /**
     * 记录当前登录管理员的操作；未认证（如无效令牌登出）时静默跳过。
     */
    public void record(String action, String targetType, String targetName) {
        try {
            if (!SecurityUtils.isAdmin()) {
                return;
            }
            long userId = SecurityUtils.currentUserId();
            recordFor(userId, resolveUsername(userId), action, targetType, targetName);
        } catch (RuntimeException ex) {
            log.warn("操作日志记录跳过（无法解析当前用户）：{}", ex.getMessage());
        }
    }

    /**
     * 指定用户的操作日志（登录时 SecurityContext 尚未建立，需显式传入）。
     */
    public void recordFor(Long userId, String username, String action, String targetType, String targetName) {
        try {
            OperationLog entry = new OperationLog();
            entry.setUserId(userId);
            entry.setUsername(truncate(username == null || username.isBlank() ? "未知用户" : username, 50));
            entry.setAction(truncate(action, 50));
            entry.setTargetType(truncate(targetType, 50));
            entry.setTargetName(truncate(targetName == null ? "" : targetName, 255));
            entry.setIp(truncate(currentIp(), IP_MAX_LENGTH));
            executor.execute(() -> insertQuietly(entry));
        } catch (RuntimeException ex) {
            log.warn("操作日志提交失败：{}", ex.getMessage());
        }
    }

    /** 当前请求的客户端 IP（兼容 Nginx 反向代理头）。 */
    public String currentIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "-";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty() && !"unknown".equalsIgnoreCase(first)) {
                return first;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp.trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "-" : remote;
    }

    private void insertQuietly(OperationLog entry) {
        try {
            operationLogMapper.insert(entry);
        } catch (RuntimeException ex) {
            log.warn("操作日志落库失败：{}", ex.getMessage());
        }
    }

    private String resolveUsername(long userId) {
        AdminUser user = adminUserMapper.selectById(userId);
        return user == null ? "用户#" + userId : user.getUsername();
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}

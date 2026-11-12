package com.yunshu.zhixue.modules.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.auth.dto.StudentLoginResponse;
import com.yunshu.zhixue.modules.student.entity.Student;
import com.yunshu.zhixue.modules.student.mapper.StudentMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogRecorder;
import com.yunshu.zhixue.security.Audience;
import com.yunshu.zhixue.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 管理端「学员端（内测账号）」免登录通道。
 *
 * <p>管理员在后台下拉菜单点击「学员端」时，为配置的内测学员账号签发学员端双令牌，
 * 前端凭令牌直接进入学员端首页，无需再次输入账号密码。</p>
 *
 * <p>安全边界：默认关闭，仅内测/开发环境通过
 * {@code app.preview.portal-login-enabled=true} 开启；账号由
 * {@code app.preview.portal-student-mobile} 指定，不接受前端传参。</p>
 */
@Service
public class PortalPreviewService {

    private final StudentMapper studentMapper;
    private final JwtTokenProvider tokenProvider;
    private final OperationLogRecorder operationLogRecorder;
    private final boolean enabled;
    private final String studentMobile;

    public PortalPreviewService(StudentMapper studentMapper,
                                JwtTokenProvider tokenProvider,
                                OperationLogRecorder operationLogRecorder,
                                @Value("${app.preview.portal-login-enabled:false}") boolean enabled,
                                @Value("${app.preview.portal-student-mobile:}") String studentMobile) {
        this.studentMapper = studentMapper;
        this.tokenProvider = tokenProvider;
        this.operationLogRecorder = operationLogRecorder;
        this.enabled = enabled;
        this.studentMobile = studentMobile;
    }

    /** 为内测学员签发学员端双令牌（调用方须为已登录管理员）。 */
    public StudentLoginResponse issueTokens() {
        if (!enabled) {
            throw new BizException(ErrorCode.NOT_FOUND, "内测学员通道未开启");
        }
        String mobile = studentMobile == null ? "" : studentMobile.trim();
        if (mobile.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "未配置内测学员账号");
        }
        Student student = studentMapper.selectOne(
                Wrappers.<Student>lambdaQuery().eq(Student::getMobile, mobile));
        if (student == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "内测学员账号不存在");
        }
        if (!student.enabled()) {
            throw new BizException(ErrorCode.STUDENT_DISABLED);
        }
        String accessToken = tokenProvider.createAccessToken(Audience.PORTAL, student.getId(), Set.of());
        String refreshToken = tokenProvider.createRefreshToken(Audience.PORTAL, student.getId());
        operationLogRecorder.record(OperationActions.OTHER, "内测学员",
                "进入学员端：" + student.getNickname());
        return new StudentLoginResponse(accessToken, refreshToken);
    }
}

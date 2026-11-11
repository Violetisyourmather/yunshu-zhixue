package com.yunshu.zhixue.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.system.dto.SystemSettingsRequest;
import com.yunshu.zhixue.modules.system.dto.SystemSettingsResponse;
import com.yunshu.zhixue.modules.system.entity.SystemSetting;
import com.yunshu.zhixue.modules.system.mapper.SystemSettingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 系统设置（A-T01 ~ A-T03）：仅基本信息 4 项，按 config_key upsert。
 */
@Service
public class SystemSettingService {

    public static final String KEY_SITE_NAME = "site_name";
    public static final String KEY_SITE_DESCRIPTION = "site_description";
    public static final String KEY_LOGO_URL = "logo_url";
    public static final String KEY_DEFAULT_AVATAR = "default_avatar";

    private static final String DEFAULT_SITE_NAME = "云枢智学";
    private static final String DEFAULT_SITE_DESCRIPTION = "AI 赋能的智能面试训练平台";

    private final SystemSettingMapper systemSettingMapper;
    private final OperationLogRecorder operationLogRecorder;

    public SystemSettingService(SystemSettingMapper systemSettingMapper,
                                OperationLogRecorder operationLogRecorder) {
        this.systemSettingMapper = systemSettingMapper;
        this.operationLogRecorder = operationLogRecorder;
    }

    /** A-T01 读取（登录即可）。 */
    public SystemSettingsResponse get() {
        Map<String, String> values = new HashMap<>();
        for (SystemSetting setting : systemSettingMapper.selectList(null)) {
            values.put(setting.getConfigKey(), setting.getConfigValue());
        }
        return new SystemSettingsResponse(
                valueOr(values, KEY_SITE_NAME, DEFAULT_SITE_NAME),
                valueOr(values, KEY_SITE_DESCRIPTION, DEFAULT_SITE_DESCRIPTION),
                valueOr(values, KEY_LOGO_URL, ""),
                valueOr(values, KEY_DEFAULT_AVATAR, ""));
    }

    /** A-T02 保存。 */
    @Transactional
    public SystemSettingsResponse save(SystemSettingsRequest request) {
        String siteName = request.siteName().trim();
        if (siteName.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "平台名称不能为空");
        }
        upsert(KEY_SITE_NAME, siteName, "平台名称");
        upsert(KEY_SITE_DESCRIPTION, trimToEmpty(request.siteDescription()), "平台描述");
        upsert(KEY_LOGO_URL, trimToEmpty(request.logoUrl()), "平台 Logo");
        upsert(KEY_DEFAULT_AVATAR, trimToEmpty(request.defaultAvatar()), "默认头像");
        operationLogRecorder.record(OperationActions.UPDATE, "系统设置", "保存基本信息");
        return get();
    }

    /** A-T03 恢复默认设置。 */
    @Transactional
    public SystemSettingsResponse reset() {
        upsert(KEY_SITE_NAME, DEFAULT_SITE_NAME, "平台名称");
        upsert(KEY_SITE_DESCRIPTION, DEFAULT_SITE_DESCRIPTION, "平台描述");
        upsert(KEY_LOGO_URL, "", "平台 Logo");
        upsert(KEY_DEFAULT_AVATAR, "", "默认头像");
        operationLogRecorder.record(OperationActions.UPDATE, "系统设置", "恢复默认设置");
        return get();
    }

    private void upsert(String key, String value, String remark) {
        SystemSetting existing = systemSettingMapper.selectOne(
                Wrappers.<SystemSetting>lambdaQuery().eq(SystemSetting::getConfigKey, key));
        if (existing == null) {
            SystemSetting setting = new SystemSetting();
            setting.setConfigKey(key);
            setting.setConfigValue(value);
            setting.setRemark(remark);
            systemSettingMapper.insert(setting);
            return;
        }
        if (!Objects.equals(existing.getConfigValue(), value) || !Objects.equals(existing.getRemark(), remark)) {
            SystemSetting update = new SystemSetting();
            update.setId(existing.getId());
            update.setConfigValue(value);
            update.setRemark(remark);
            systemSettingMapper.updateById(update);
        }
    }

    private String valueOr(Map<String, String> values, String key, String defaultValue) {
        String value = values.get(key);
        return value == null ? defaultValue : value;
    }

    private static String trimToEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}

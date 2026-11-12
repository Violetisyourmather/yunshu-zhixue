package com.yunshu.zhixue.common.constant;

import java.time.ZoneId;
import java.util.List;

/**
 * 固定阈值常量（D13：均为固定常量，不做后台可配置）。
 * 数值与需求文档第 5 章、01 §2.3 保持一致。
 */
public final class AppConstants {

    private AppConstants() {
    }

    /** 业务时区：每日配额与「今日」口径按上海自然日 0 点（D17）。 */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    // ---------- AI 面试 ----------
    public static final int INTERVIEW_DEFAULT_QUESTION_COUNT = 10;
    public static final List<Integer> INTERVIEW_QUESTION_COUNT_OPTIONS = List.of(3, 5, 8, 10, 15);
    /** 单题回答超时：固定 5 分钟。 */
    public static final int INTERVIEW_QUESTION_TIMEOUT_SECONDS = 300;
    /** 每日 AI 面试次数上限，次日 0 点重置。 */
    public static final int INTERVIEW_DAILY_QUOTA = 5;

    // ---------- 刷题 ----------
    /** 简答题通过线：得分 ≥6（满分 10）。 */
    public static final int SHORT_ANSWER_PASS_SCORE = 6;
    /** 错题连续答对 3 次自动标记已掌握。 */
    public static final int WRONG_QUESTION_MASTER_THRESHOLD = 3;

    // ---------- 账号安全与频控（D18） ----------
    public static final int SMS_CODE_LENGTH = 6;
    public static final int SMS_CODE_TTL_MINUTES = 5;
    public static final int SMS_RESEND_COOLDOWN_SECONDS = 60;
    public static final int SMS_DAILY_LIMIT = 10;
    /** 找回密码一次性令牌有效期。 */
    public static final int RESET_TOKEN_TTL_MINUTES = 10;
    public static final int LOGIN_MAX_FAILURES = 5;
    public static final int LOGIN_LOCK_MINUTES = 5;
    public static final int VERIFY_CODE_MAX_FAILURES = 5;
    /** 通用接口频控窗口：60 秒（按 IP 与账号双维度分别计数，阈值不向前端暴露）。 */
    public static final int RATE_LIMIT_WINDOW_SECONDS = 60;
    public static final int RATE_LIMIT_LOGIN = 20;
    public static final int RATE_LIMIT_LOGIN_SMS = 10;
    public static final int RATE_LIMIT_REGISTER = 10;
    public static final int RATE_LIMIT_RESET = 10;
    public static final int RATE_LIMIT_SMS = 10;
    public static final int RATE_LIMIT_INTERVIEW_START = 10;
    public static final int RATE_LIMIT_ADMIN_LOGIN = 20;

    // ---------- 导入 ----------
    public static final int IMPORT_MAX_ROWS = 500;
    /** 导入预览暂存 TTL：30 分钟（两段式导入的幂等窗口）。 */
    public static final int IMPORT_PREVIEW_TTL_MINUTES = 30;

    // ---------- 系统管理 ----------
    /** 内置超级管理员角色编码（03 种子数据，仅该角色可访问角色管理）。 */
    public static final String SUPER_ADMIN_ROLE_CODE = "super_admin";

    /** 运营人员角色编码（需求 4.5.2：除系统管理外的全部操作）。 */
    public static final String OPERATOR_ROLE_CODE = "operator";

    /** 内容审核角色编码（需求 4.5.2：只查看内容并审核题目）。 */
    public static final String CONTENT_REVIEWER_ROLE_CODE = "content_reviewer";
}

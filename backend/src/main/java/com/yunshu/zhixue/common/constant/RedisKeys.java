package com.yunshu.zhixue.common.constant;

/**
 * Redis 键约定（04 §2.12）。所有键集中在常量类中，禁止散落拼接。
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 验证码发送冷却：60 秒 1 次。 */
    public static final String SMS_COOLDOWN = "sms:cooldown:";
    /** 验证码每日发送上限：每日 ≤10 次。 */
    public static final String SMS_DAY_COUNT = "sms:daycount:";
    /** 验证码连续校验失败计数。 */
    public static final String SMS_VERIFY_FAIL = "sms:verifyfail:";
    /** 登录失败计数：连续 5 次锁 5 分钟。 */
    public static final String LOGIN_FAIL = "login:fail:";
    /** 管理端登录失败计数：与学员端账号体系隔离。 */
    public static final String ADMIN_LOGIN_FAIL_PREFIX = LOGIN_FAIL + "admin:";
    /** 令牌黑名单：登出后 jti 失效。 */
    public static final String TOKEN_BLACKLIST = "token:blacklist:";
    /** 通用接口频控。 */
    public static final String RATE_LIMIT = "rate:";
    /** 找回密码一次性令牌。 */
    public static final String RESET_TOKEN = "reset:token:";
    /** 面试会话暂存（题目快照 + 逐题得分，TTL 24h）。 */
    public static final String INTERVIEW_TEMP = "interview:temp:";
    /** 幂等键（TTL 24h）。 */
    public static final String IDEMPOTENCY = "idempotency:";

    /** 验证码：{@code sms:code:{type}:{mobile}}，type ∈ register/login/reset。 */
    public static String smsCode(String type, String mobile) {
        return "sms:code:" + type + ":" + mobile;
    }

    /** 学员端登录失败计数：{@code login:fail:{account}}（account 为手机号或邮箱）。 */
    public static String portalLoginFail(String account) {
        return LOGIN_FAIL + account;
    }

    /** 管理端登录失败计数：{@code login:fail:admin:{account}}。 */
    public static String adminLoginFail(String account) {
        return ADMIN_LOGIN_FAIL_PREFIX + account;
    }

    /** 面试每日配额：{@code interview:quota:{studentId}:{yyyyMMdd}}（上海日期）。 */
    public static String interviewQuota(long studentId, String shanghaiDate) {
        return "interview:quota:" + studentId + ":" + shanghaiDate;
    }
}

package com.yunshu.zhixue.common.constant;

/**
 * 操作日志动作枚举（04 附录 C：API 取值 = DB 存储值，中文直传）。
 */
public final class OperationActions {

    private OperationActions() {
    }

    public static final String LOGIN = "登录";
    public static final String LOGOUT = "登出";
    public static final String CREATE = "新增";
    public static final String UPDATE = "修改";
    public static final String DELETE = "删除";
    public static final String STATUS = "禁用启用";
    public static final String EXPORT = "导出";
    public static final String OTHER = "其他";
}

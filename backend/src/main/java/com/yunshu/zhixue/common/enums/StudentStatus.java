package com.yunshu.zhixue.common.enums;

/** 学员 / 后台账号状态（student.status / admin_user.status）。 */
public enum StudentStatus implements CodedEnum {

    DISABLED(0, "已禁用"),
    ENABLED(1, "已启用");

    private final int code;
    private final String label;

    StudentStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String label() {
        return label;
    }

    public static StudentStatus fromCode(Integer code) {
        return CodedEnum.fromCode(StudentStatus.class, code);
    }
}

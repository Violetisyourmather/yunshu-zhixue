package com.yunshu.zhixue.common.enums;

/** 性别（student.gender，默认 0 保密）。 */
public enum Gender implements CodedEnum {

    UNKNOWN(0, "保密"),
    MALE(1, "男"),
    FEMALE(2, "女");

    private final int code;
    private final String label;

    Gender(int code, String label) {
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

    public static Gender fromCode(Integer code) {
        return CodedEnum.fromCode(Gender.class, code);
    }
}

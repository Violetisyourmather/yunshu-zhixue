package com.yunshu.zhixue.common.enums;

/** 练习模式（practice_record.mode）。 */
public enum PracticeMode implements CodedEnum {

    SEQUENTIAL(1, "顺序练习"),
    RANDOM(2, "随机练习");

    private final int code;
    private final String label;

    PracticeMode(int code, String label) {
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

    public static PracticeMode fromCode(Integer code) {
        return CodedEnum.fromCode(PracticeMode.class, code);
    }
}

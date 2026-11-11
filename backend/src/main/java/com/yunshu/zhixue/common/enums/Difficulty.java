package com.yunshu.zhixue.common.enums;

/** 难度（question.difficulty / interview_question.difficulty / interview_record.difficulty）。 */
public enum Difficulty implements CodedEnum {

    EASY(1, "简单"),
    MEDIUM(2, "中等"),
    HARD(3, "困难");

    private final int code;
    private final String label;

    Difficulty(int code, String label) {
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

    public static Difficulty fromCode(Integer code) {
        return CodedEnum.fromCode(Difficulty.class, code);
    }
}

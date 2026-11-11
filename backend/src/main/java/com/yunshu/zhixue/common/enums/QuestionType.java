package com.yunshu.zhixue.common.enums;

/** 题目类型（question.type）。 */
public enum QuestionType implements CodedEnum {

    SINGLE_CHOICE(1, "单选"),
    MULTIPLE_CHOICE(2, "多选"),
    TRUE_FALSE(3, "判断"),
    SHORT_ANSWER(4, "简答");

    private final int code;
    private final String label;

    QuestionType(int code, String label) {
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

    public static QuestionType fromCode(Integer code) {
        return CodedEnum.fromCode(QuestionType.class, code);
    }

    /** 客观题（即时判分，不调用 AI）。 */
    public boolean isObjective() {
        return this != SHORT_ANSWER;
    }
}

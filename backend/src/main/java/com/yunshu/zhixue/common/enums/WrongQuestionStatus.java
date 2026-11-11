package com.yunshu.zhixue.common.enums;

/** 错题状态（wrong_question.status）：连续答对 3 次自动转为已掌握。 */
public enum WrongQuestionStatus implements CodedEnum {

    PENDING_REVIEW(1, "待复习"),
    MASTERED(2, "已掌握");

    private final int code;
    private final String label;

    WrongQuestionStatus(int code, String label) {
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

    public static WrongQuestionStatus fromCode(Integer code) {
        return CodedEnum.fromCode(WrongQuestionStatus.class, code);
    }
}

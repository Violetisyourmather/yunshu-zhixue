package com.yunshu.zhixue.common.enums;

/** 面试题状态（interview_question.status，D3-2）；AI 面试仅取已上架题。 */
public enum InterviewQuestionStatus implements CodedEnum {

    PUBLISHED(1, "已上架"),
    OFFLINE(2, "已下架");

    private final int code;
    private final String label;

    InterviewQuestionStatus(int code, String label) {
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

    public static InterviewQuestionStatus fromCode(Integer code) {
        return CodedEnum.fromCode(InterviewQuestionStatus.class, code);
    }
}

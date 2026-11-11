package com.yunshu.zhixue.common.enums;

/** 面试题来源（interview_question.source_type）。 */
public enum InterviewQuestionSource implements CodedEnum {

    DOC_PARSE(1, "文档解析"),
    MANUAL(2, "手工录入"),
    MARKDOWN_IMPORT(3, "手动导入");

    private final int code;
    private final String label;

    InterviewQuestionSource(int code, String label) {
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

    public static InterviewQuestionSource fromCode(Integer code) {
        return CodedEnum.fromCode(InterviewQuestionSource.class, code);
    }
}

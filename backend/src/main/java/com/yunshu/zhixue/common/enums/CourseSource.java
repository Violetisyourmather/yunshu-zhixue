package com.yunshu.zhixue.common.enums;

/** 课程来源（course.source）。 */
public enum CourseSource implements CodedEnum {

    BILIBILI(1, "B站"),
    OTHER(2, "其他");

    private final int code;
    private final String label;

    CourseSource(int code, String label) {
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

    public static CourseSource fromCode(Integer code) {
        return CodedEnum.fromCode(CourseSource.class, code);
    }
}

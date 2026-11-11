package com.yunshu.zhixue.common.enums;

/** 课程状态（course.status，D3-7）；学员端仅展示上架课程。 */
public enum CourseStatus implements CodedEnum {

    ONLINE(1, "上架"),
    OFFLINE(2, "下架");

    private final int code;
    private final String label;

    CourseStatus(int code, String label) {
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

    public static CourseStatus fromCode(Integer code) {
        return CodedEnum.fromCode(CourseStatus.class, code);
    }
}

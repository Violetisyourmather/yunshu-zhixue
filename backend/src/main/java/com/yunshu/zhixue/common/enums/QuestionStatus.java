package com.yunshu.zhixue.common.enums;

/**
 * 题目状态（question.status，默认 3 待审核）。
 *
 * <p>{@code 4 草稿} 为预留值，当前不启用：接口不接受该入参，筛选下拉不展示（Q6）。</p>
 */
public enum QuestionStatus implements CodedEnum {

    PUBLISHED(1, "已上架"),
    OFFLINE(2, "已下架"),
    PENDING_REVIEW(3, "待审核"),
    DRAFT(4, "草稿"),
    REJECTED(5, "已驳回");

    private final int code;
    private final String label;

    QuestionStatus(int code, String label) {
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

    public static QuestionStatus fromCode(Integer code) {
        return CodedEnum.fromCode(QuestionStatus.class, code);
    }

    /** 学员端仅消费已上架题目。 */
    public boolean visibleToPortal() {
        return this == PUBLISHED;
    }
}

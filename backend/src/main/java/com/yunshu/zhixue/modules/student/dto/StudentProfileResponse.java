package com.yunshu.zhixue.modules.student.dto;

/**
 * P-M01 学员个人信息。
 *
 * @param mobile 脱敏手机号，如 138****1234
 */
public record StudentProfileResponse(
        long id,
        String nickname,
        String mobile,
        String email,
        int gender,
        Long preferredSubjectId,
        String avatar,
        boolean guideFinished,
        Stats stats,
        Study study) {

    /** 用户卡片三项统计。 */
    public record Stats(long practiceCount, long interviewCount, long wrongQuestionCount) {
    }

    /** 学习数据：累计学习天数、正确率与已学课程数。 */
    public record Study(long totalDays, double accuracyRate, long completedCourseCount) {
    }
}

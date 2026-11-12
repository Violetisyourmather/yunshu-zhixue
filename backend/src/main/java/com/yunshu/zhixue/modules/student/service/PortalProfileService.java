package com.yunshu.zhixue.modules.student.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.student.dto.ProfileUpdateRequest;
import com.yunshu.zhixue.modules.student.dto.StudentCountRow;
import com.yunshu.zhixue.modules.student.dto.StudentPracticeStatRow;
import com.yunshu.zhixue.modules.student.dto.StudentProfileResponse;
import com.yunshu.zhixue.modules.student.entity.Student;
import com.yunshu.zhixue.modules.student.mapper.StudentMapper;
import com.yunshu.zhixue.security.SecurityUtils;
import com.yunshu.zhixue.support.storage.MinioProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 学员端个人中心（P-M01 / P-M02 / P-M04）。
 *
 * <p>学习数据沿用学员管理模块的统计口径：作答记录、面试记录为事实来源，
 * 查询时聚合，不重复存储。</p>
 */
@Service
public class PortalProfileService {

    private static final int NICKNAME_MAX_LENGTH = 20;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final StudentMapper studentMapper;
    private final MinioProperties minioProperties;

    public PortalProfileService(StudentMapper studentMapper, MinioProperties minioProperties) {
        this.studentMapper = studentMapper;
        this.minioProperties = minioProperties;
    }

    /** P-M01 读取当前学员资料与学习统计。 */
    public StudentProfileResponse getProfile() {
        Student student = currentStudent();
        List<Long> ids = List.of(student.getId());

        StudentPracticeStatRow practice = studentMapper.selectPracticeStats(ids).stream()
                .findFirst()
                .orElse(null);
        long practiceCount = practice == null ? 0L : nullSafe(practice.getTotal());
        long correctCount = practice == null ? 0L : nullSafe(practice.getCorrect());
        long totalDays = firstTotal(studentMapper.selectLearningDays(ids));
        long interviewCount = firstTotal(studentMapper.selectInterviewCounts(ids));
        long completedCourseCount = firstTotal(studentMapper.selectCompletionCounts(ids));
        long wrongQuestionCount = studentMapper.countWrongQuestions(student.getId());

        return new StudentProfileResponse(
                student.getId(),
                student.getNickname(),
                maskMobile(student.getMobile()),
                student.getEmail(),
                nullSafe(student.getGender()),
                student.getPreferredSubjectId(),
                student.getAvatar(),
                student.getGuideFinished() != null && student.getGuideFinished() == 1,
                new StudentProfileResponse.Stats(practiceCount, interviewCount, wrongQuestionCount),
                new StudentProfileResponse.Study(totalDays, accuracyRate(practiceCount, correctCount),
                        completedCourseCount));
    }

    /**
     * P-M02 修改资料。
     *
     * <p>未传字段保持原值；邮箱/头像传空字符串时按 NULL 写入。邮箱先做应用层唯一预检，
     * 并发情况下由 {@code uk_student_email} 兜底转为 20001。</p>
     */
    @Transactional
    public void updateProfile(ProfileUpdateRequest request) {
        Student student = currentStudent();
        var update = Wrappers.<Student>lambdaUpdate().eq(Student::getId, student.getId());
        boolean changed = false;

        if (request.nickname() != null) {
            String nickname = request.nickname().trim();
            if (nickname.isEmpty() || nickname.length() > NICKNAME_MAX_LENGTH) {
                throw new BizException(ErrorCode.PARAM_INVALID, "用户名长度为 1~20 个字符");
            }
            update.set(Student::getNickname, nickname);
            changed = true;
        }

        if (request.email() != null) {
            String email = request.email().trim();
            if (email.isEmpty()) {
                update.set(Student::getEmail, null);
                changed = true;
            } else {
                if (!EMAIL_PATTERN.matcher(email).matches()) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "邮箱格式不正确");
                }
                long occupied = studentMapper.selectCount(Wrappers.<Student>lambdaQuery()
                        .eq(Student::getEmail, email)
                        .ne(Student::getId, student.getId()));
                if (occupied > 0) {
                    throw new BizException(ErrorCode.CONTACT_OCCUPIED, "该邮箱已被其他账号使用");
                }
                update.set(Student::getEmail, email);
                changed = true;
            }
        }

        if (request.gender() != null) {
            update.set(Student::getGender, request.gender());
            changed = true;
        }

        if (request.preferredSubjectId() != null) {
            update.set(Student::getPreferredSubjectId, request.preferredSubjectId());
            changed = true;
        }

        if (request.avatar() != null) {
            String avatar = request.avatar().trim();
            if (avatar.isEmpty()) {
                update.set(Student::getAvatar, null);
                changed = true;
            } else {
                if (!isAllowedAvatar(avatar)) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "头像地址不合法");
                }
                update.set(Student::getAvatar, avatar);
                changed = true;
            }
        }

        if (!changed) {
            return;
        }
        try {
            studentMapper.update(null, update);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.CONTACT_OCCUPIED, "该邮箱已被其他账号使用");
        }
    }

    /** P-M04 完成新手引导，幂等。 */
    @Transactional
    public void completeGuide() {
        Student student = currentStudent();
        if (student.getGuideFinished() != null && student.getGuideFinished() == 1) {
            return;
        }
        studentMapper.update(null, Wrappers.<Student>lambdaUpdate()
                .eq(Student::getId, student.getId())
                .set(Student::getGuideFinished, 1));
    }

    private Student currentStudent() {
        Student student = studentMapper.selectById(SecurityUtils.currentUserId());
        if (student == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!student.enabled()) {
            throw new BizException(ErrorCode.STUDENT_DISABLED);
        }
        return student;
    }

    private boolean isAllowedAvatar(String avatar) {
        if (avatar.startsWith("/files/")) {
            return true;
        }
        String publicUrl = minioProperties.getPublicUrl();
        if (publicUrl == null || publicUrl.isBlank()) {
            return false;
        }
        String prefix = publicUrl.endsWith("/") ? publicUrl : publicUrl + "/";
        return avatar.startsWith(prefix);
    }

    private static String maskMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return "";
        }
        if (mobile.length() <= 4) {
            return "****";
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    private static long firstTotal(List<StudentCountRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0L;
        }
        return nullSafe(rows.get(0).getTotal());
    }

    private static double accuracyRate(long total, long correct) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round(correct * 1000.0 / total) / 10.0;
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}

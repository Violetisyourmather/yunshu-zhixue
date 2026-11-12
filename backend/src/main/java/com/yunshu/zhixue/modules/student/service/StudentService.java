package com.yunshu.zhixue.modules.student.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.student.dto.StudentBatchStatusRequest;
import com.yunshu.zhixue.modules.student.dto.StudentCountRow;
import com.yunshu.zhixue.modules.student.dto.StudentDetail;
import com.yunshu.zhixue.modules.student.dto.StudentInterviewItem;
import com.yunshu.zhixue.modules.student.dto.StudentInterviewRow;
import com.yunshu.zhixue.modules.student.dto.StudentItem;
import com.yunshu.zhixue.modules.student.dto.StudentPracticeStatRow;
import com.yunshu.zhixue.modules.student.entity.Student;
import com.yunshu.zhixue.modules.student.mapper.StudentMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogRecorder;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学员管理（A-U01 ~ A-U08）。
 *
 * <p>学员由前台注册产生，后台只做查看、启禁用、删除与导出；
 * 学习数据全部由 practice_record / interview_record / route_course_completion 聚合。</p>
 */
@Service
public class StudentService {

    private static final int PAGE_SIZE_MAX = 100;
    private static final int INTERVIEW_PAGE_SIZE_MAX = 50;
    private static final int EXPORT_MAX_ROWS = 10000;
    private static final String TARGET = "学员";
    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StudentMapper studentMapper;
    private final OperationLogRecorder operationLogRecorder;

    public StudentService(StudentMapper studentMapper,
                          OperationLogRecorder operationLogRecorder) {
        this.studentMapper = studentMapper;
        this.operationLogRecorder = operationLogRecorder;
    }

    /** A-U01 列表：昵称 / 手机号搜索 + 状态筛选。 */
    public PageResult<StudentItem> list(String keyword, Integer status, long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), PAGE_SIZE_MAX);
        Page<Student> result = studentMapper.selectPage(
                new Page<>(safePage, safePageSize),
                buildQuery(keyword, status).orderByDesc(Student::getId));
        Stats stats = loadStats(result.getRecords().stream().map(Student::getId).toList());
        List<StudentItem> items = result.getRecords().stream()
                .map(student -> new StudentItem(student.getId(), student.getNickname(),
                        student.getMobile(), student.getAvatar(), nullSafe(student.getStatus()),
                        student.getCreatedAt(), student.getLastLoginAt(),
                        count(stats.learningDays(), student.getId()),
                        statTotal(stats.practice(), student.getId()),
                        count(stats.interviews(), student.getId())))
                .toList();
        return PageResult.of(items, result.getTotal(), safePage, safePageSize);
    }

    /** A-U06 详情。 */
    public StudentDetail detail(Long id) {
        Student student = requireStudent(id);
        Stats stats = loadStats(List.of(id));
        long done = statTotal(stats.practice(), id);
        long correct = statCorrect(stats.practice(), id);
        return new StudentDetail(student.getId(), student.getNickname(), student.getMobile(),
                student.getEmail(), student.getAvatar(), nullSafe(student.getGender()),
                nullSafe(student.getStatus()), student.getCreatedAt(), student.getLastLoginAt(),
                count(stats.learningDays(), id), count(stats.completions(), id),
                done, accuracyRate(done, correct), count(stats.interviews(), id));
    }

    /** A-U07 学员面试记录分页。 */
    public PageResult<StudentInterviewItem> interviews(Long studentId, long page, long pageSize) {
        requireStudent(studentId);
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), INTERVIEW_PAGE_SIZE_MAX);
        long total = studentMapper.countInterviews(studentId);
        long offset = (safePage - 1) * safePageSize;
        List<StudentInterviewItem> items = total == 0 ? List.of()
                : studentMapper.selectInterviews(studentId, offset, safePageSize).stream()
                .map(this::toInterviewItem)
                .toList();
        return PageResult.of(items, total, safePage, safePageSize);
    }

    /** A-U02 启用 / 禁用学员。 */
    @Transactional
    public void updateStatus(Long id, Integer status) {
        Student student = requireStudent(id);
        requireStatus(status);
        studentMapper.update(null, Wrappers.<Student>lambdaUpdate()
                .eq(Student::getId, id)
                .set(Student::getStatus, status));
        operationLogRecorder.record(OperationActions.STATUS, TARGET,
                student.getNickname() + (status == 1 ? " 启用" : " 禁用"));
    }

    /** A-U03 批量启用 / 禁用。 */
    @Transactional
    public int batchStatus(StudentBatchStatusRequest request) {
        requireStatus(request.status());
        List<Student> students = studentMapper.selectBatchIds(request.ids());
        if (students.isEmpty()) {
            return 0;
        }
        studentMapper.update(null, Wrappers.<Student>lambdaUpdate()
                .in(Student::getId, students.stream().map(Student::getId).toList())
                .set(Student::getStatus, request.status()));
        operationLogRecorder.record(OperationActions.STATUS, TARGET,
                (request.status() == 1 ? "批量启用 " : "批量禁用 ") + students.size() + " 名学员");
        return students.size();
    }

    /** A-U04 删除学员（逻辑删除，保留学习记录）。 */
    @Transactional
    public void delete(Long id) {
        Student student = requireStudent(id);
        releaseUniqueFields(student);
        studentMapper.deleteById(id);
        operationLogRecorder.record(OperationActions.DELETE, TARGET, student.getNickname());
    }

    /** A-U05 批量删除学员。 */
    @Transactional
    public void batchDelete(Collection<Long> ids) {
        List<Student> students = studentMapper.selectBatchIds(ids);
        if (students.isEmpty()) {
            return;
        }
        for (Student student : students) {
            releaseUniqueFields(student);
        }
        studentMapper.deleteBatchIds(students.stream().map(Student::getId).toList());
        operationLogRecorder.record(OperationActions.DELETE, TARGET,
                "批量删除 " + students.size() + " 名学员");
    }

    /** A-U08 按当前筛选条件导出学员 Excel。 */
    public void export(HttpServletResponse response, String keyword, Integer status) {
        var query = buildQuery(keyword, status).orderByDesc(Student::getId);
        long total = studentMapper.selectCount(query);
        if (total > EXPORT_MAX_ROWS) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "导出数量超过 " + EXPORT_MAX_ROWS + " 条，请缩小筛选范围");
        }
        List<Student> students = studentMapper.selectList(query);
        Stats stats = loadStats(students.stream().map(Student::getId).toList());

        List<List<String>> head = List.of(
                List.of("昵称"), List.of("手机号"), List.of("邮箱"), List.of("注册时间"),
                List.of("最后登录"), List.of("累计学习天数"), List.of("已学课程"),
                List.of("刷题总数"), List.of("正确率"), List.of("面试次数"), List.of("状态"));
        List<List<String>> rows = students.stream().map(student -> {
            long done = statTotal(stats.practice(), student.getId());
            long correct = statCorrect(stats.practice(), student.getId());
            return List.of(
                    nullToEmpty(student.getNickname()),
                    nullToEmpty(student.getMobile()),
                    nullToEmpty(student.getEmail()),
                    formatDateTime(student.getCreatedAt()),
                    formatDateTime(student.getLastLoginAt()),
                    String.valueOf(count(stats.learningDays(), student.getId())),
                    String.valueOf(count(stats.completions(), student.getId())),
                    String.valueOf(done),
                    formatAccuracy(accuracyRate(done, correct)),
                    String.valueOf(count(stats.interviews(), student.getId())),
                    nullSafe(student.getStatus()) == 1 ? "已启用" : "已禁用");
        }).toList();

        try {
            response.setContentType(CONTENT_TYPE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                    + URLEncoder.encode("学员列表.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
            EasyExcel.write(response.getOutputStream())
                    .head(head)
                    .sheet("学员")
                    .doWrite(rows);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "文件生成失败");
        }
        operationLogRecorder.record(OperationActions.EXPORT, TARGET,
                "导出学员 " + rows.size() + " 名");
    }

    private LambdaQueryWrapper<Student> buildQuery(String keyword, Integer status) {
        var query = Wrappers.<Student>lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(w -> w.like(Student::getNickname, value).or().like(Student::getMobile, value));
        }
        if (status != null) {
            query.eq(Student::getStatus, status);
        }
        return query;
    }

    private Stats loadStats(Collection<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return new Stats(Map.of(), Map.of(), Map.of(), Map.of());
        }
        Map<Long, StudentPracticeStatRow> practice = studentMapper.selectPracticeStats(studentIds).stream()
                .collect(Collectors.toMap(StudentPracticeStatRow::getId, Function.identity(), (a, b) -> a));
        return new Stats(practice,
                toCountMap(studentMapper.selectLearningDays(studentIds)),
                toCountMap(studentMapper.selectInterviewCounts(studentIds)),
                toCountMap(studentMapper.selectCompletionCounts(studentIds)));
    }

    private Map<Long, StudentCountRow> toCountMap(List<StudentCountRow> rows) {
        return rows.stream().collect(Collectors.toMap(StudentCountRow::getId,
                Function.identity(), (a, b) -> a));
    }

    private StudentInterviewItem toInterviewItem(StudentInterviewRow row) {
        return new StudentInterviewItem(row.getId(), nullSafe(row.getSubjectId()),
                row.getSubjectName(), nullSafe(row.getDifficulty()),
                nullSafe(row.getQuestionCount()),
                row.getOverallScore() == null ? null : row.getOverallScore().doubleValue(),
                row.getTotalDuration(), row.getStartedAt(), row.getCompletedAt());
    }

    /**
     * 逻辑删除前释放唯一键：手机号改为 {@code d{id}}，邮箱置 NULL，
     * 避免已删除学员占用手机号/邮箱导致前台无法重新注册。
     */
    private void releaseUniqueFields(Student student) {
        String released = "d" + student.getId();
        if (released.length() > 11) {
            released = released.substring(released.length() - 11);
        }
        studentMapper.update(null, Wrappers.<Student>lambdaUpdate()
                .eq(Student::getId, student.getId())
                .set(Student::getMobile, released)
                .set(Student::getEmail, null));
    }

    private Student requireStudent(Long id) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学员不存在");
        }
        return student;
    }

    private static void requireStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "状态值不合法");
        }
    }

    private static long statTotal(Map<Long, StudentPracticeStatRow> stats, Long studentId) {
        StudentPracticeStatRow row = stats.get(studentId);
        return row == null || row.getTotal() == null ? 0L : row.getTotal();
    }

    private static long statCorrect(Map<Long, StudentPracticeStatRow> stats, Long studentId) {
        StudentPracticeStatRow row = stats.get(studentId);
        return row == null || row.getCorrect() == null ? 0L : row.getCorrect();
    }

    private static long count(Map<Long, StudentCountRow> stats, Long studentId) {
        StudentCountRow row = stats.get(studentId);
        return row == null || row.getTotal() == null ? 0L : row.getTotal();
    }

    static double accuracyRate(long done, long correct) {
        if (done <= 0) {
            return 0.0;
        }
        return Math.round(correct * 1000.0 / done) / 10.0;
    }

    private static String formatAccuracy(double accuracy) {
        return accuracy + "%";
    }

    private static String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(AppConstants.BUSINESS_ZONE)
                .format(DATE_TIME_FORMATTER);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private record Stats(Map<Long, StudentPracticeStatRow> practice,
                         Map<Long, StudentCountRow> learningDays,
                         Map<Long, StudentCountRow> interviews,
                         Map<Long, StudentCountRow> completions) {
    }
}

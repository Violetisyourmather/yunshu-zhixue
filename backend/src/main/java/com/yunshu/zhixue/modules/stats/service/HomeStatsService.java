package com.yunshu.zhixue.modules.stats.service;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.stats.dto.DailyPracticeCountRow;
import com.yunshu.zhixue.modules.stats.dto.HotSubjectItem;
import com.yunshu.zhixue.modules.stats.dto.HotSubjectRow;
import com.yunshu.zhixue.modules.stats.dto.LearningOverview;
import com.yunshu.zhixue.modules.stats.dto.LearningStats;
import com.yunshu.zhixue.modules.stats.dto.PlatformStats;
import com.yunshu.zhixue.modules.stats.dto.PracticeSummaryRow;
import com.yunshu.zhixue.modules.stats.dto.StudentGuideRow;
import com.yunshu.zhixue.modules.stats.dto.WeekTrendPoint;
import com.yunshu.zhixue.modules.stats.mapper.HomeStatsMapper;
import com.yunshu.zhixue.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 首页数据（P-H01 ~ P-H03）。
 *
 * <p>只读服务：平台数据与热门学科公开，学习概览仅当前登录学员可读。
 * 学习统计全部由 practice_record 明细聚合，口径与需求文档 §3.5 补充 G6 一致。</p>
 */
@Service
public class HomeStatsService {

    private static final int HOT_SUBJECT_LIMIT = 6;
    private static final int WEEK_DAYS = 7;

    private final HomeStatsMapper homeStatsMapper;

    public HomeStatsService(HomeStatsMapper homeStatsMapper) {
        this.homeStatsMapper = homeStatsMapper;
    }

    /** P-H01 平台数据（公开）。 */
    public PlatformStats platformStats() {
        return new PlatformStats(
                homeStatsMapper.countStudents(),
                homeStatsMapper.countPublishedInterviewQuestions(),
                homeStatsMapper.countCompletedInterviews());
    }

    /** P-H02 热门学科（公开，按题目数倒序取 6 个）。 */
    public List<HotSubjectItem> hotSubjects() {
        return homeStatsMapper.selectHotSubjects(HOT_SUBJECT_LIMIT).stream()
                .map(this::toHotSubjectItem)
                .toList();
    }

    /** P-H03 学习概览（登录）。 */
    public LearningOverview learningOverview() {
        long studentId = SecurityUtils.currentUserId();
        StudentGuideRow student = homeStatsMapper.selectStudentGuide(studentId);
        if (student == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (student.getStatus() == null || student.getStatus() != 1) {
            throw new BizException(ErrorCode.STUDENT_DISABLED);
        }

        PracticeSummaryRow summary = homeStatsMapper.selectPracticeSummary(studentId);
        long totalPracticeCount = nullSafe(summary == null ? null : summary.getTotal());
        long objectiveTotal = nullSafe(summary == null ? null : summary.getObjectiveTotal());
        long objectiveCorrect = nullSafe(summary == null ? null : summary.getObjectiveCorrect());

        WeekResult week = buildWeekTrend(studentId);
        LearningStats learningStats = new LearningStats(
                consecutiveDays(studentId),
                accuracyRate(objectiveTotal, objectiveCorrect),
                totalPracticeCount);

        return new LearningOverview(learningStats, week.points(), week.todayCount(),
                student.getGuideFinished() != null && student.getGuideFinished() == 1);
    }

    private HotSubjectItem toHotSubjectItem(HotSubjectRow row) {
        return new HotSubjectItem(
                nullSafe(row.getSubjectId()),
                row.getSubjectName(),
                row.getIcon(),
                nullSafe(row.getStageCount()),
                nullSafe(row.getQuestionCount()));
    }

    /**
     * 连续学习天数：从今天（今天无记录则从昨天）向前，连续存在作答记录的自然日数。
     *
     * <p>只统计刷题产生的记录，课程完成勾选不计入；日期按 Asia/Shanghai 自然日。</p>
     */
    private long consecutiveDays(long studentId) {
        List<String> rawDates = homeStatsMapper.selectPracticeDatesDesc(studentId);
        if (rawDates.isEmpty()) {
            return 0;
        }
        Set<LocalDate> dates = rawDates.stream()
                .map(LocalDate::parse)
                .collect(Collectors.toSet());
        LocalDate cursor = LocalDate.now(AppConstants.BUSINESS_ZONE);
        if (!dates.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        long streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    /** 近 7 天练习量：补全没有作答记录的日期，最后一项固定为今天。 */
    private WeekResult buildWeekTrend(long studentId) {
        LocalDate today = LocalDate.now(AppConstants.BUSINESS_ZONE);
        LocalDate start = today.minusDays(WEEK_DAYS - 1L);
        LocalDateTime startUtc = start.atStartOfDay(AppConstants.BUSINESS_ZONE)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();

        Map<String, Long> counts = homeStatsMapper.selectDailyPracticeCounts(studentId, startUtc).stream()
                .filter(row -> row.getPracticeDate() != null)
                .collect(Collectors.toMap(DailyPracticeCountRow::getPracticeDate,
                        row -> nullSafe(row.getTotal()), (first, ignored) -> first));

        List<WeekTrendPoint> points = new ArrayList<>(WEEK_DAYS);
        long todayCount = 0;
        for (int index = 0; index < WEEK_DAYS; index++) {
            LocalDate date = start.plusDays(index);
            long count = counts.getOrDefault(date.toString(), 0L);
            points.add(new WeekTrendPoint(date.toString(), count));
            if (date.equals(today)) {
                todayCount = count;
            }
        }
        return new WeekResult(points, todayCount);
    }

    /** 正确率：客观题答对数 ÷ 客观题作答数，0~100，保留 1 位小数。 */
    private static double accuracyRate(long objectiveTotal, long objectiveCorrect) {
        if (objectiveTotal <= 0) {
            return 0.0;
        }
        return Math.round(objectiveCorrect * 1000.0 / objectiveTotal) / 10.0;
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private record WeekResult(List<WeekTrendPoint> points, long todayCount) {
    }
}

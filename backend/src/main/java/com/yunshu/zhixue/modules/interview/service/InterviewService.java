package com.yunshu.zhixue.modules.interview.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.ai.InterviewAiService;
import com.yunshu.zhixue.modules.ai.dto.InterviewAnswerGrade;
import com.yunshu.zhixue.modules.ai.dto.InterviewImproveCard;
import com.yunshu.zhixue.modules.ai.dto.InterviewQaSnapshot;
import com.yunshu.zhixue.modules.ai.dto.InterviewReportAi;
import com.yunshu.zhixue.modules.interview.dto.InterviewAnswerData;
import com.yunshu.zhixue.modules.interview.dto.InterviewAnswerRequest;
import com.yunshu.zhixue.modules.interview.dto.InterviewConfigResponse;
import com.yunshu.zhixue.modules.interview.dto.InterviewFinishRequest;
import com.yunshu.zhixue.modules.interview.dto.InterviewFinishResponse;
import com.yunshu.zhixue.modules.interview.dto.InterviewQuestionItem;
import com.yunshu.zhixue.modules.interview.dto.InterviewQuestionReview;
import com.yunshu.zhixue.modules.interview.dto.InterviewRecordItem;
import com.yunshu.zhixue.modules.interview.dto.InterviewReportResponse;
import com.yunshu.zhixue.modules.interview.dto.InterviewStartRequest;
import com.yunshu.zhixue.modules.interview.dto.InterviewStartResponse;
import com.yunshu.zhixue.modules.interview.dto.InterviewStats;
import com.yunshu.zhixue.modules.interview.dto.InterviewSubjectOption;
import com.yunshu.zhixue.modules.interview.dto.InterviewTempGrade;
import com.yunshu.zhixue.modules.interview.dto.InterviewTempQuestion;
import com.yunshu.zhixue.modules.interview.dto.InterviewTempSession;
import com.yunshu.zhixue.modules.interview.entity.InterviewQuestionScore;
import com.yunshu.zhixue.modules.interview.entity.InterviewRecord;
import com.yunshu.zhixue.modules.interview.entity.InterviewReport;
import com.yunshu.zhixue.modules.interview.mapper.InterviewQuestionScoreMapper;
import com.yunshu.zhixue.modules.interview.mapper.InterviewRecordMapper;
import com.yunshu.zhixue.modules.interview.mapper.InterviewReportMapper;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewQuestion;
import com.yunshu.zhixue.modules.interviewbank.mapper.InterviewQuestionMapper;
import com.yunshu.zhixue.modules.student.entity.Student;
import com.yunshu.zhixue.modules.student.mapper.StudentMapper;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.security.RequestRateLimiter;
import com.yunshu.zhixue.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学员端 AI 面试（P-I01 ~ P-I05、P-I07 ~ P-I08）。
 *
 * <p>发起面试只写 Redis 暂存并扣减当日配额；逐题评分写 Redis；
 * 结束时一次性落库并生成六维报告。</p>
 */
@Service
public class InterviewService {

    private static final int PAGE_SIZE_DEFAULT = 10;
    private static final int PAGE_SIZE_MAX = 50;
    /** 面试方向：8 个 AI 方向 + Java后端开发（新增素材方向），按学科排序取前 9 个。 */
    private static final int DIRECTION_COUNT = 9;

    private final SubjectMapper subjectMapper;
    private final InterviewQuestionMapper interviewQuestionMapper;
    private final InterviewTempStore tempStore;
    private final InterviewQuotaService quotaService;
    private final InterviewAiService aiService;
    private final InterviewRecordMapper recordMapper;
    private final InterviewQuestionScoreMapper scoreMapper;
    private final InterviewReportMapper reportMapper;
    private final InterviewPersistenceService persistenceService;
    private final StudentMapper studentMapper;
    private final ObjectMapper objectMapper;
    private final RequestRateLimiter rateLimiter;

    public InterviewService(SubjectMapper subjectMapper,
                            InterviewQuestionMapper interviewQuestionMapper,
                            InterviewTempStore tempStore,
                            InterviewQuotaService quotaService,
                            InterviewAiService aiService,
                            InterviewRecordMapper recordMapper,
                            InterviewQuestionScoreMapper scoreMapper,
                            InterviewReportMapper reportMapper,
                            InterviewPersistenceService persistenceService,
                            StudentMapper studentMapper,
                            ObjectMapper objectMapper,
                            RequestRateLimiter rateLimiter) {
        this.subjectMapper = subjectMapper;
        this.interviewQuestionMapper = interviewQuestionMapper;
        this.tempStore = tempStore;
        this.quotaService = quotaService;
        this.aiService = aiService;
        this.recordMapper = recordMapper;
        this.scoreMapper = scoreMapper;
        this.reportMapper = reportMapper;
        this.persistenceService = persistenceService;
        this.studentMapper = studentMapper;
        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
    }

    /** P-I01 面试配置。 */
    public InterviewConfigResponse config() {
        long studentId = currentStudentId();
        List<InterviewSubjectOption> subjects = subjectMapper.selectList(
                        Wrappers.<Subject>lambdaQuery()
                                .orderByAsc(Subject::getSortOrder)
                                .orderByAsc(Subject::getId)).stream()
                .limit(DIRECTION_COUNT)
                .map(subject -> new InterviewSubjectOption(subject.getId(), subject.getName()))
                .toList();
        return new InterviewConfigResponse(
                subjects,
                List.of(1, 2, 3),
                AppConstants.INTERVIEW_QUESTION_COUNT_OPTIONS,
                AppConstants.INTERVIEW_DEFAULT_QUESTION_COUNT,
                quotaService.remaining(studentId),
                AppConstants.INTERVIEW_QUESTION_TIMEOUT_SECONDS / 60);
    }

    /** P-I02 发起面试。 */
    public InterviewStartResponse start(InterviewStartRequest request) {
        long studentId = currentStudentId();
        rateLimiter.check("interview-start", AppConstants.RATE_LIMIT_INTERVIEW_START,
                "user:" + studentId);
        Subject subject = subjectMapper.selectById(request.subjectId());
        if (subject == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "面试方向不存在");
        }
        if (!AppConstants.INTERVIEW_QUESTION_COUNT_OPTIONS.contains(request.count())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "题目数量不合法");
        }
        if (quotaService.remaining(studentId) <= 0) {
            throw new BizException(ErrorCode.INTERVIEW_QUOTA_EXHAUSTED);
        }

        List<InterviewQuestion> pool = interviewQuestionMapper.selectList(
                Wrappers.<InterviewQuestion>lambdaQuery()
                        .eq(InterviewQuestion::getSubjectId, request.subjectId())
                        .eq(InterviewQuestion::getDifficulty, request.difficulty())
                        .eq(InterviewQuestion::getStatus, 1));
        if (pool.size() < request.count()) {
            throw new BizException(ErrorCode.INTERVIEW_QUESTION_INSUFFICIENT,
                    "该方向可出题数量不足，题目筹备中");
        }
        List<InterviewQuestion> selected = new ArrayList<>(pool);
        Collections.shuffle(selected);
        selected = selected.subList(0, request.count());

        InterviewTempSession temp = new InterviewTempSession();
        temp.setSessionId(UUID.randomUUID().toString());
        temp.setStudentId(studentId);
        temp.setSubjectId(subject.getId());
        temp.setSubjectName(subject.getName());
        temp.setDifficulty(request.difficulty());
        temp.setQuestionCount(selected.size());
        temp.setStartedAt(LocalDateTime.now(ZoneOffset.UTC));
        List<InterviewTempQuestion> questions = new ArrayList<>();
        for (int index = 0; index < selected.size(); index++) {
            InterviewQuestion question = selected.get(index);
            questions.add(new InterviewTempQuestion(index + 1, question.getId(),
                    question.getContent(), question.getReferenceAnswer(), request.difficulty()));
        }
        temp.setQuestions(questions);
        tempStore.save(temp);
        try {
            quotaService.consume(studentId);
        } catch (RuntimeException ex) {
            tempStore.delete(temp.getSessionId());
            throw ex;
        }

        List<InterviewQuestionItem> items = questions.stream()
                .map(question -> new InterviewQuestionItem(
                        question.order(), subject.getId(), question.difficulty(), question.content()))
                .toList();
        return new InterviewStartResponse(temp.getSessionId(), items, items.size());
    }

    /** 校验当前学员与面试会话，供 SSE 控制器在开流前调用。 */
    public InterviewTempSession requireSession(String sessionId) {
        long studentId = currentStudentId();
        InterviewTempSession session = tempStore.load(sessionId);
        if (session == null || session.getStudentId() != studentId) {
            throw new BizException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND);
        }
        return session;
    }

    public void validateOrder(InterviewTempSession session, int order) {
        boolean exists = session.getQuestions().stream()
                .anyMatch(question -> question.order() == order);
        if (!exists) {
            throw new BizException(ErrorCode.NOT_FOUND, "面试题目不存在");
        }
    }

    /**
     * P-I03 单题 AI 评分，并把得分写入 Redis 暂存。
     *
     * <p>由 SSE 异步线程调用，因此显式传入已在请求线程校验过的 studentId。</p>
     */
    public InterviewAnswerGrade gradeAnswer(String sessionId, InterviewAnswerRequest request,
                                            long studentId) {
        InterviewTempSession session = tempStore.load(sessionId);
        if (session == null || session.getStudentId() != studentId) {
            throw new BizException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND);
        }
        InterviewTempQuestion question = session.getQuestions().stream()
                .filter(item -> item.order() == request.order())
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "面试题目不存在"));
        InterviewAnswerGrade grade = aiService.gradeAnswer(
                question.content(), question.referenceAnswer(), request.studentAnswer());
        session.getGrades().put(request.order(), new InterviewTempGrade(
                request.order(), grade.score(), grade.feedback(),
                grade.degraded(), grade.referencePoints()));
        tempStore.save(session);
        return grade;
    }

    /** P-I04 结束面试并生成报告。 */
    public InterviewFinishResponse finish(String sessionId, InterviewFinishRequest request) {
        long studentId = currentStudentId();
        InterviewTempSession session = tempStore.load(sessionId);
        if (session == null || session.getStudentId() != studentId) {
            throw new BizException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND);
        }
        LocalDateTime completedAt = LocalDateTime.now(ZoneOffset.UTC);
        Map<Integer, InterviewAnswerRequest> answersByOrder = request.answers().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(InterviewAnswerRequest::order, Function.identity(),
                        (first, ignored) -> first, LinkedHashMap::new));

        List<InterviewAnswerData> answerData = new ArrayList<>();
        List<InterviewQaSnapshot> snapshots = new ArrayList<>();
        int durationSum = 0;
        for (InterviewTempQuestion question : session.getQuestions()) {
            InterviewAnswerRequest answer = answersByOrder.get(question.order());
            String studentAnswer = answer == null || answer.studentAnswer() == null
                    ? null
                    : answer.studentAnswer().trim();
            int duration = answer == null || answer.durationSeconds() == null
                    ? 0
                    : Math.max(0, answer.durationSeconds());
            durationSum += duration;
            InterviewTempGrade grade = session.getGrades().get(question.order());
            double score = grade == null ? 0.0 : grade.score();
            String feedback = grade == null ? "未作答" : grade.feedback();
            answerData.add(new InterviewAnswerData(question.order(), question.questionId(),
                    question.content(), studentAnswer, duration, score, feedback));
            snapshots.add(new InterviewQaSnapshot(question.order(), question.content(),
                    question.referenceAnswer(), studentAnswer, score, feedback));
        }
        int totalDuration = durationSum > 0
                ? durationSum
                : (int) Math.max(0, Duration.between(session.getStartedAt(), completedAt).getSeconds());

        InterviewReportAi report = aiService.generateReport(
                session.getSubjectName(), session.getDifficulty(), snapshots);
        long reportId = persistenceService.persist(
                session, answerData, report, totalDuration, completedAt);
        tempStore.delete(sessionId);
        return new InterviewFinishResponse(reportId, report.overallScore());
    }

    /** P-I05 报告详情。 */
    public InterviewReportResponse report(long reportId) {
        long studentId = currentStudentId();
        InterviewReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "面试报告不存在");
        }
        InterviewRecord record = recordMapper.selectById(report.getInterviewRecordId());
        if (record == null || record.getStudentId() == null || record.getStudentId() != studentId) {
            throw new BizException(ErrorCode.NOT_FOUND, "面试报告不存在");
        }
        Subject subject = subjectMapper.selectById(record.getSubjectId());
        Double trendDelta = null;
        if (record.getOverallScore() != null) {
            InterviewRecord previous = recordMapper.selectPrevious(studentId, record.getSubjectId(),
                    record.getCompletedAt(), record.getId());
            if (previous != null && previous.getOverallScore() != null) {
                trendDelta = round(record.getOverallScore().doubleValue()
                        - previous.getOverallScore().doubleValue());
            }
        }
        List<InterviewQuestionScore> scores = scoreMapper.selectList(
                Wrappers.<InterviewQuestionScore>lambdaQuery()
                        .eq(InterviewQuestionScore::getInterviewRecordId, record.getId())
                        .orderByAsc(InterviewQuestionScore::getQuestionOrder));
        List<InterviewQuestionReview> reviews = scores.stream()
                .map(score -> new InterviewQuestionReview(
                        nullSafe(score.getQuestionOrder()),
                        score.getQuestionText(),
                        score.getStudentAnswer(),
                        score.getScore() == null ? 0.0 : score.getScore().doubleValue(),
                        score.getAiFeedback()))
                .toList();

        return new InterviewReportResponse(
                report.getId(),
                record.getId(),
                nullSafe(record.getSubjectId()),
                subject == null ? null : subject.getName(),
                nullSafe(record.getDifficulty()),
                nullSafe(record.getQuestionCount()),
                record.getOverallScore() == null ? 0.0 : record.getOverallScore().doubleValue(),
                trendDelta,
                readDimensions(report.getRadarJson()),
                readList(report.getStrengths()),
                readList(report.getWeaknesses()),
                readList(report.getSuggestions()),
                readCards(report.getImprovementCardsJson()),
                reviews,
                nullSafe(record.getTotalDuration()),
                record.getCompletedAt());
    }

    /** P-I07 面试记录分页。 */
    public PageResult<InterviewRecordItem> records(long page, long pageSize) {
        long studentId = currentStudentId();
        long safePage = Math.max(page, 1);
        long safePageSize = pageSize <= 0
                ? PAGE_SIZE_DEFAULT
                : Math.min(pageSize, PAGE_SIZE_MAX);
        Page<InterviewRecord> result = recordMapper.selectPage(
                new Page<>(safePage, safePageSize),
                Wrappers.<InterviewRecord>lambdaQuery()
                        .eq(InterviewRecord::getStudentId, studentId)
                        .orderByDesc(InterviewRecord::getCompletedAt)
                        .orderByDesc(InterviewRecord::getId));
        List<InterviewRecord> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(List.of(), result.getTotal(), safePage, safePageSize);
        }
        Set<Long> subjectIds = records.stream().map(InterviewRecord::getSubjectId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Subject> subjects = subjectIds.isEmpty() ? Map.of()
                : subjectMapper.selectBatchIds(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity(), (a, b) -> a));
        Set<Long> recordIds = records.stream().map(InterviewRecord::getId).collect(Collectors.toSet());
        Map<Long, Long> reportByRecord = reportMapper.selectList(
                        Wrappers.<InterviewReport>lambdaQuery()
                                .in(InterviewReport::getInterviewRecordId, recordIds)).stream()
                .collect(Collectors.toMap(InterviewReport::getInterviewRecordId,
                        InterviewReport::getId, (a, b) -> a));

        List<InterviewRecordItem> items = records.stream().map(record -> {
            Subject subject = record.getSubjectId() == null ? null : subjects.get(record.getSubjectId());
            Long reportId = reportByRecord.get(record.getId());
            return new InterviewRecordItem(
                    record.getId(), reportId,
                    nullSafe(record.getSubjectId()),
                    subject == null ? null : subject.getName(),
                    nullSafe(record.getDifficulty()),
                    nullSafe(record.getQuestionCount()),
                    record.getOverallScore() == null ? null : record.getOverallScore().doubleValue(),
                    record.getTotalDuration(),
                    record.getCompletedAt(),
                    reportId != null);
        }).toList();
        return PageResult.of(items, result.getTotal(), safePage, safePageSize);
    }

    /** P-I08 面试统计，分数口径 0~10。 */
    public InterviewStats recordsStats() {
        long studentId = currentStudentId();
        List<InterviewRecord> records = recordMapper.selectList(
                Wrappers.<InterviewRecord>lambdaQuery()
                        .eq(InterviewRecord::getStudentId, studentId));
        if (records.isEmpty()) {
            return new InterviewStats(0, null, null, 0);
        }
        double sum = 0;
        double max = 0;
        int scored = 0;
        long duration = 0;
        for (InterviewRecord record : records) {
            if (record.getOverallScore() != null) {
                double value = record.getOverallScore().doubleValue();
                sum += value;
                max = Math.max(max, value);
                scored++;
            }
            duration += nullSafe(record.getTotalDuration());
        }
        Double avg = scored == 0 ? null : round(sum / scored);
        Double maxScore = scored == 0 ? null : round(max);
        return new InterviewStats(records.size(), avg, maxScore, duration);
    }

    private long currentStudentId() {
        long studentId = SecurityUtils.currentUserId();
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!student.enabled()) {
            throw new BizException(ErrorCode.STUDENT_DISABLED);
        }
        return studentId;
    }

    private Map<String, Double> readDimensions(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Double>>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<InterviewImproveCard> readCards(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<InterviewImproveCard>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private static int nullSafe(BigDecimal value) {
        return value == null ? 0 : value.intValue();
    }
}

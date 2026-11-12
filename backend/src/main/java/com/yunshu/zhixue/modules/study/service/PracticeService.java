package com.yunshu.zhixue.modules.study.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.ai.ShortAnswerGrader;
import com.yunshu.zhixue.modules.ai.dto.AiPoint;
import com.yunshu.zhixue.modules.ai.dto.ShortAnswerGrade;
import com.yunshu.zhixue.modules.question.dto.QuestionStatRow;
import com.yunshu.zhixue.modules.question.entity.Question;
import com.yunshu.zhixue.modules.question.entity.QuestionOption;
import com.yunshu.zhixue.modules.question.mapper.QuestionMapper;
import com.yunshu.zhixue.modules.question.mapper.QuestionOptionMapper;
import com.yunshu.zhixue.modules.question.service.QuestionService;
import com.yunshu.zhixue.modules.student.entity.Student;
import com.yunshu.zhixue.modules.student.mapper.StudentMapper;
import com.yunshu.zhixue.modules.study.dto.PracticeAnswer;
import com.yunshu.zhixue.modules.study.dto.PracticeGradedAnswer;
import com.yunshu.zhixue.modules.study.dto.PracticeOptionItem;
import com.yunshu.zhixue.modules.study.dto.PracticeQuestionItem;
import com.yunshu.zhixue.modules.study.dto.PracticeResultItem;
import com.yunshu.zhixue.modules.study.dto.PracticeStartRequest;
import com.yunshu.zhixue.modules.study.dto.PracticeStartResponse;
import com.yunshu.zhixue.modules.study.dto.PracticeSubmitRequest;
import com.yunshu.zhixue.modules.study.dto.PracticeSubmitResponse;
import com.yunshu.zhixue.modules.subject.entity.Stage;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.StageMapper;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学员端刷题（P-P01 / P-P02）。
 *
 * <p>客观题由服务端按题目答案重新判定，简答题经 AI 批改；判分完成后统一写入作答明细，
 * 并维护错题本状态。练习会话本身只存在前端本地。</p>
 */
@Service
public class PracticeService {

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final StageMapper stageMapper;
    private final SubjectMapper subjectMapper;
    private final StudentMapper studentMapper;
    private final ShortAnswerGrader shortAnswerGrader;
    private final PracticePersistenceService persistenceService;

    public PracticeService(QuestionMapper questionMapper,
                           QuestionOptionMapper questionOptionMapper,
                           StageMapper stageMapper,
                           SubjectMapper subjectMapper,
                           StudentMapper studentMapper,
                           ShortAnswerGrader shortAnswerGrader,
                           PracticePersistenceService persistenceService) {
        this.questionMapper = questionMapper;
        this.questionOptionMapper = questionOptionMapper;
        this.stageMapper = stageMapper;
        this.subjectMapper = subjectMapper;
        this.studentMapper = studentMapper;
        this.shortAnswerGrader = shortAnswerGrader;
        this.persistenceService = persistenceService;
    }

    /** P-P01 组卷并返回题目（答案随题返回用于前端即时判定）。 */
    public PracticeStartResponse start(PracticeStartRequest request) {
        currentStudentId();
        int mode = request.mode();
        String source = request.source();
        if ("wrong_redo".equals(source)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "错题重做将在错题本模块开放");
        }

        List<Question> selected;
        String title;
        if ("question".equals(source)) {
            if (request.questionId() == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "请选择题目的 id");
            }
            Question question = questionMapper.selectById(request.questionId());
            if (question == null || !Objects.equals(question.getStatus(), QuestionService.STATUS_ONLINE)) {
                throw new BizException(ErrorCode.NOT_FOUND, "题目不存在或未上架");
            }
            requireActiveStage(question.getStageId());
            selected = List.of(question);
            title = "单题练习";
        } else {
            validateFilters(request.subjectId(), request.stageId(), request.type(), request.difficulty());
            title = resolveTitle(request, mode);
            var query = Wrappers.<Question>lambdaQuery()
                    .eq(Question::getStatus, QuestionService.STATUS_ONLINE);
            String stageCondition = "SELECT id FROM stage WHERE delete_flag = 0 AND status = 1";
            if (request.stageId() != null) {
                query.eq(Question::getStageId, request.stageId());
            } else if (request.subjectId() != null) {
                stageCondition += " AND subject_id = " + request.subjectId();
            }
            query.inSql(Question::getStageId, stageCondition);
            if (request.type() != null) {
                query.eq(Question::getType, request.type());
            }
            if (request.difficulty() != null) {
                query.eq(Question::getDifficulty, request.difficulty());
            }
            query.orderByAsc(Question::getId);
            selected = questionMapper.selectList(query);

            if (mode == 2) {
                selected = new ArrayList<>(selected);
                Collections.shuffle(selected);
                int count = request.count() == null ? 10 : request.count();
                if (selected.size() > count) {
                    selected = new ArrayList<>(selected.subList(0, count));
                }
            } else if (request.count() != null && selected.size() > request.count()) {
                selected = new ArrayList<>(selected.subList(0, request.count()));
            }
        }

        if (selected.isEmpty()) {
            throw new BizException(ErrorCode.INTERVIEW_QUESTION_INSUFFICIENT, "当前范围暂无可练习的题目");
        }
        return buildStartResponse(selected, title);
    }

    /**
     * P-W03 错题重做组卷：按错题本选中的题目 id 顺序返回，过滤已下架 / 已停用题目。
     */
    public PracticeStartResponse startWrongRedo(List<Long> questionIds) {
        currentStudentId();
        if (questionIds == null || questionIds.isEmpty()) {
            throw new BizException(ErrorCode.INTERVIEW_QUESTION_INSUFFICIENT, "没有可重做的错题");
        }
        Set<Long> orderedIds = questionIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Question> questions = questionMapper.selectBatchIds(orderedIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
        Map<Long, Stage> stages = loadStages(questions.values().stream()
                .map(Question::getStageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        List<Question> selected = orderedIds.stream()
                .map(questions::get)
                .filter(Objects::nonNull)
                .filter(question -> Objects.equals(question.getStatus(), QuestionService.STATUS_ONLINE))
                .filter(question -> {
                    Stage stage = stages.get(question.getStageId());
                    return stage != null && Objects.equals(stage.getStatus(), 1);
                })
                .toList();
        if (selected.isEmpty()) {
            throw new BizException(ErrorCode.INTERVIEW_QUESTION_INSUFFICIENT, "没有可重做的错题");
        }
        return buildStartResponse(selected, "错题重做");
    }

    /** P-P02 重新判分、AI 批改并落库。 */
    public PracticeSubmitResponse submit(String practiceId, PracticeSubmitRequest request) {
        long studentId = currentStudentId();
        int mode = request.mode() == null ? 1 : request.mode();

        Set<Long> questionIds = request.answers().stream()
                .map(PracticeAnswer::questionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Question> questions = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
        Set<Long> stageIds = questions.values().stream()
                .map(Question::getStageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Stage> stages = stageIds.isEmpty() ? Map.of()
                : stageMapper.selectBatchIds(stageIds).stream()
                .collect(Collectors.toMap(Stage::getId, Function.identity(), (a, b) -> a));
        Map<Long, List<QuestionOption>> optionsByQuestion = questionOptionMapper.selectList(
                        Wrappers.<QuestionOption>lambdaQuery()
                                .in(QuestionOption::getQuestionId, questionIds)
                                .orderByAsc(QuestionOption::getSortOrder)
                                .orderByAsc(QuestionOption::getId)).stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));

        List<PracticeResultItem> results = new ArrayList<>(request.answers().size());
        List<PracticeGradedAnswer> gradedAnswers = new ArrayList<>(request.answers().size());
        for (PracticeAnswer answer : request.answers()) {
            Question question = questions.get(answer.questionId());
            if (question == null || !Objects.equals(question.getStatus(), QuestionService.STATUS_ONLINE)) {
                throw new BizException(ErrorCode.NOT_FOUND, "题目不存在或未上架");
            }
            Stage stage = stages.get(question.getStageId());
            if (stage == null || !Objects.equals(stage.getStatus(), 1)) {
                throw new BizException(ErrorCode.NOT_FOUND, "题目所在阶段已停用");
            }
            List<QuestionOption> options = optionsByQuestion.getOrDefault(question.getId(), List.of());
            int type = nullSafe(question.getType());
            String studentAnswer = answer.answer() == null ? "" : answer.answer().trim();
            String correctAnswer = resolveAnswer(question, options);

            boolean correct;
            double score;
            String aiFeedback = null;
            List<AiPoint> points = List.of();
            boolean degraded = false;

            if (type == QuestionService.TYPE_SHORT) {
                ShortAnswerGrade grade = shortAnswerGrader.grade(
                        question.getContent(), question.getAnswer(), studentAnswer);
                correct = grade.score() >= AppConstants.SHORT_ANSWER_PASS_SCORE;
                score = grade.score();
                aiFeedback = grade.feedback();
                points = grade.points();
                degraded = grade.degraded();
            } else {
                correct = isObjectiveCorrect(question, options, studentAnswer);
                score = correct ? 1.0 : 0.0;
            }

            results.add(new PracticeResultItem(
                    question.getId(), correct, score, correctAnswer,
                    question.getAnalysis(), aiFeedback, points, degraded));
            gradedAnswers.add(new PracticeGradedAnswer(
                    question.getId(), correct, answer.durationSeconds(), studentAnswer));
        }

        persistenceService.persist(studentId, mode, gradedAnswers);
        return new PracticeSubmitResponse(results);
    }

    private PracticeStartResponse buildStartResponse(List<Question> questions, String title) {
        Map<Long, List<QuestionOption>> optionsByQuestion = questionOptionMapper.selectList(
                        Wrappers.<QuestionOption>lambdaQuery()
                                .in(QuestionOption::getQuestionId,
                                        questions.stream().map(Question::getId).toList())
                                .orderByAsc(QuestionOption::getSortOrder)
                                .orderByAsc(QuestionOption::getId)).stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));
        Map<Long, Stage> stages = loadStages(questions.stream().map(Question::getStageId).toList());
        Map<Long, Subject> subjects = loadSubjects(stages.values().stream()
                .map(Stage::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<Long, QuestionStatRow> stats = questionMapper.selectPracticeStats(
                        questions.stream().map(Question::getId).toList()).stream()
                .collect(Collectors.toMap(QuestionStatRow::getId, Function.identity(), (a, b) -> a));

        List<PracticeQuestionItem> items = questions.stream().map(question -> {
            List<QuestionOption> options = optionsByQuestion.getOrDefault(question.getId(), List.of());
            Stage stage = stages.get(question.getStageId());
            Subject subject = stage == null ? null : subjects.get(stage.getSubjectId());
            QuestionStatRow stat = stats.get(question.getId());
            long done = stat == null || stat.getTotal() == null ? 0L : stat.getTotal();
            long correctCount = stat == null || stat.getCorrect() == null ? 0L : stat.getCorrect();
            return new PracticeQuestionItem(
                    question.getId(),
                    nullSafe(question.getType()),
                    nullSafe(question.getDifficulty()),
                    question.getContent(),
                    options.stream()
                            .map(option -> new PracticeOptionItem(option.getOptionKey(), option.getOptionText()))
                            .toList(),
                    resolveAnswer(question, options),
                    question.getAnalysis(),
                    stage == null ? null : stage.getName(),
                    subject == null ? null : subject.getName(),
                    question.getTags(),
                    done,
                    accuracyRate(done, correctCount));
        }).toList();

        return new PracticeStartResponse(UUID.randomUUID().toString(), title, items);
    }

    private String resolveTitle(PracticeStartRequest request, int mode) {
        if (request.stageId() != null) {
            return requireActiveStage(request.stageId()).getName();
        }
        if (request.subjectId() != null) {
            Subject subject = subjectMapper.selectById(request.subjectId());
            if (subject == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "学科不存在");
            }
            return subject.getName();
        }
        return mode == 2 ? "随机练习" : "综合练习";
    }

    private Stage requireActiveStage(Long stageId) {
        Stage stage = stageMapper.selectById(stageId);
        if (stage == null || !Objects.equals(stage.getStatus(), 1)) {
            throw new BizException(ErrorCode.NOT_FOUND, "阶段不存在或已停用");
        }
        return stage;
    }

    private Map<Long, Stage> loadStages(Collection<Long> stageIds) {
        if (stageIds == null || stageIds.isEmpty()) {
            return Map.of();
        }
        return stageMapper.selectBatchIds(stageIds).stream()
                .collect(Collectors.toMap(Stage::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, Subject> loadSubjects(Collection<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return Map.of();
        }
        return subjectMapper.selectBatchIds(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity(), (a, b) -> a));
    }

    private boolean isObjectiveCorrect(Question question, List<QuestionOption> options, String studentAnswer) {
        int type = nullSafe(question.getType());
        if (type == QuestionService.TYPE_SINGLE || type == QuestionService.TYPE_MULTI) {
            Set<String> expected = options.stream()
                    .filter(option -> Integer.valueOf(1).equals(option.getIsCorrect()))
                    .map(QuestionOption::getOptionKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Set<String> actual = parseKeys(studentAnswer);
            return !expected.isEmpty() && expected.equals(actual);
        }
        if (type == QuestionService.TYPE_JUDGE) {
            String expected = normalizeJudge(question.getAnswer());
            String actual = normalizeJudge(studentAnswer);
            return expected != null && expected.equals(actual);
        }
        return false;
    }

    /** 从 "A,C" / "AC" / "A C" 中提取选项标识。 */
    private static Set<String> parseKeys(String value) {
        Set<String> keys = new LinkedHashSet<>();
        if (value == null) {
            return keys;
        }
        for (char ch : value.toUpperCase(Locale.ROOT).toCharArray()) {
            if (ch >= 'A' && ch <= 'Z') {
                keys.add(String.valueOf(ch));
            }
        }
        return keys;
    }

    private static String normalizeJudge(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if ("正确".equals(trimmed) || "对".equals(trimmed) || "√".equals(trimmed)
                || "true".equalsIgnoreCase(trimmed) || "是".equals(trimmed)) {
            return "正确";
        }
        if ("错误".equals(trimmed) || "错".equals(trimmed) || "×".equals(trimmed)
                || "false".equalsIgnoreCase(trimmed) || "否".equals(trimmed)) {
            return "错误";
        }
        return null;
    }

    /** 选择题返回正确选项键（如 A,C），判断 / 简答返回答案文本。 */
    private static String resolveAnswer(Question question, List<QuestionOption> options) {
        int type = nullSafe(question.getType());
        if (type == QuestionService.TYPE_SINGLE || type == QuestionService.TYPE_MULTI) {
            String keys = options.stream()
                    .filter(option -> Integer.valueOf(1).equals(option.getIsCorrect()))
                    .map(QuestionOption::getOptionKey)
                    .collect(Collectors.joining(","));
            return keys.isEmpty() ? question.getAnswer() : keys;
        }
        return question.getAnswer();
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

    private static void validateFilters(Long subjectId, Long stageId, Integer type, Integer difficulty) {
        if (subjectId != null && subjectId <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "学科参数不合法");
        }
        if (stageId != null && stageId <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "阶段参数不合法");
        }
        if (type != null && (type < 1 || type > 4)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "题型参数不合法");
        }
        if (difficulty != null && (difficulty < 1 || difficulty > 3)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "难度参数不合法");
        }
    }

    private static double accuracyRate(long done, long correct) {
        if (done <= 0) {
            return 0.0;
        }
        return Math.round(correct * 1000.0 / done) / 10.0;
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}

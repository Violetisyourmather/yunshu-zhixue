package com.yunshu.zhixue.modules.study.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.question.entity.Question;
import com.yunshu.zhixue.modules.question.entity.QuestionOption;
import com.yunshu.zhixue.modules.question.mapper.QuestionMapper;
import com.yunshu.zhixue.modules.question.mapper.QuestionOptionMapper;
import com.yunshu.zhixue.modules.student.entity.Student;
import com.yunshu.zhixue.modules.student.mapper.StudentMapper;
import com.yunshu.zhixue.modules.study.dto.PracticeOptionItem;
import com.yunshu.zhixue.modules.study.dto.PracticeStartResponse;
import com.yunshu.zhixue.modules.study.dto.WrongQuestionItem;
import com.yunshu.zhixue.modules.study.dto.WrongQuestionStats;
import com.yunshu.zhixue.modules.study.dto.WrongRedoRequest;
import com.yunshu.zhixue.modules.study.entity.WrongQuestion;
import com.yunshu.zhixue.modules.study.mapper.WrongQuestionMapper;
import com.yunshu.zhixue.modules.subject.entity.Stage;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.StageMapper;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.question.service.QuestionService;
import com.yunshu.zhixue.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学员端错题本（P-W01 ~ P-W05）。
 *
 * <p>错题本以 wrong_question 为事实来源；题目内容与选项只读引用题库。
 * 已掌握错题仍展示，但不再提供「标记已掌握」按钮；再次答错由作答流程回退待复习。</p>
 */
@Service
public class WrongQuestionService {

    private static final int PAGE_SIZE_DEFAULT = 10;
    private static final int PAGE_SIZE_MAX = 100;

    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final StageMapper stageMapper;
    private final SubjectMapper subjectMapper;
    private final StudentMapper studentMapper;
    private final PracticeService practiceService;

    public WrongQuestionService(WrongQuestionMapper wrongQuestionMapper,
                                QuestionMapper questionMapper,
                                QuestionOptionMapper questionOptionMapper,
                                StageMapper stageMapper,
                                SubjectMapper subjectMapper,
                                StudentMapper studentMapper,
                                PracticeService practiceService) {
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.questionMapper = questionMapper;
        this.questionOptionMapper = questionOptionMapper;
        this.stageMapper = stageMapper;
        this.subjectMapper = subjectMapper;
        this.studentMapper = studentMapper;
        this.practiceService = practiceService;
    }

    /** P-W01 统计卡 + 高频错题 Top3。 */
    public WrongQuestionStats stats() {
        long studentId = currentStudentId();
        List<WrongQuestion> all = wrongQuestionMapper.selectList(Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getStudentId, studentId));
        long total = all.size();
        long toReview = all.stream()
                .filter(wrong -> nullSafe(wrong.getStatus()) == 1)
                .count();
        long mastered = all.stream()
                .filter(wrong -> nullSafe(wrong.getStatus()) == 2)
                .count();
        double rate = total == 0 ? 0.0 : Math.round(mastered * 1000.0 / total) / 10.0;

        Comparator<WrongQuestion> byCount = Comparator
                .comparingInt((WrongQuestion wrong) -> nullSafe(wrong.getErrorCount()))
                .reversed()
                .thenComparing(recentComparator());
        List<WrongQuestion> topRows = all.stream()
                .filter(wrong -> nullSafe(wrong.getStatus()) == 1)
                .sorted(byCount)
                .limit(3)
                .toList();
        Map<Long, Question> questions = loadQuestions(topRows.stream()
                .map(WrongQuestion::getQuestionId)
                .collect(Collectors.toSet()));
        List<WrongQuestionStats.WrongTopItem> top3 = topRows.stream()
                .filter(wrong -> questions.containsKey(wrong.getQuestionId()))
                .map(wrong -> new WrongQuestionStats.WrongTopItem(
                        wrong.getId(),
                        wrong.getQuestionId(),
                        questions.get(wrong.getQuestionId()).getContent(),
                        nullSafe(wrong.getErrorCount())))
                .toList();

        return new WrongQuestionStats(total, toReview, mastered, rate, top3);
    }

    /** P-W02 错题列表：状态 / 题型 / 时间 / 排序筛选。 */
    public PageResult<WrongQuestionItem> list(Integer status, Integer type, Integer days,
                                              String sort, long page, long pageSize) {
        long studentId = currentStudentId();
        validateFilters(status, type, days);
        long safePage = Math.max(page, 1);
        long safePageSize = pageSize <= 0
                ? PAGE_SIZE_DEFAULT
                : Math.min(pageSize, PAGE_SIZE_MAX);

        LocalDateTime threshold = days == null
                ? null
                : LocalDateTime.now(ZoneOffset.UTC).minusDays(days);
        List<WrongQuestion> candidates = wrongQuestionMapper.selectList(
                        Wrappers.<WrongQuestion>lambdaQuery()
                                .eq(WrongQuestion::getStudentId, studentId)).stream()
                .filter(wrong -> status == null || Objects.equals(wrong.getStatus(), status))
                .filter(wrong -> threshold == null
                        || (wrong.getLastWrongAt() != null && !wrong.getLastWrongAt().isBefore(threshold)))
                .toList();

        Map<Long, Question> questions = loadQuestions(candidates.stream()
                .map(WrongQuestion::getQuestionId)
                .collect(Collectors.toSet()));
        Comparator<WrongQuestion> comparator = "count".equalsIgnoreCase(sort)
                ? Comparator.comparingInt((WrongQuestion wrong) -> nullSafe(wrong.getErrorCount()))
                .reversed()
                .thenComparing(recentComparator())
                : recentComparator();

        List<WrongQuestion> filtered = candidates.stream()
                .filter(wrong -> questions.containsKey(wrong.getQuestionId()))
                .filter(wrong -> type == null
                        || Objects.equals(questions.get(wrong.getQuestionId()).getType(), type))
                .sorted(comparator)
                .toList();
        long total = filtered.size();
        int from = (int) Math.min((safePage - 1) * safePageSize, total);
        int to = (int) Math.min(from + safePageSize, total);
        List<WrongQuestion> pageRows = from >= to ? List.of() : filtered.subList(from, to);

        Set<Long> questionIds = pageRows.stream()
                .map(WrongQuestion::getQuestionId)
                .collect(Collectors.toSet());
        Map<Long, List<QuestionOption>> optionsByQuestion = loadOptions(questionIds);
        Map<Long, Stage> stages = loadStages(pageRows.stream()
                .map(wrong -> questions.get(wrong.getQuestionId()))
                .filter(Objects::nonNull)
                .map(Question::getStageId)
                .collect(Collectors.toSet()));
        Map<Long, Subject> subjects = loadSubjects(stages.values().stream()
                .map(Stage::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        List<WrongQuestionItem> items = pageRows.stream().map(wrong -> {
            Question question = questions.get(wrong.getQuestionId());
            List<QuestionOption> options = optionsByQuestion.getOrDefault(question.getId(), List.of());
            Stage stage = stages.get(question.getStageId());
            Subject subject = stage == null ? null : subjects.get(stage.getSubjectId());
            return new WrongQuestionItem(
                    wrong.getId(),
                    question.getId(),
                    nullSafe(question.getType()),
                    question.getContent(),
                    options.stream()
                            .map(option -> new PracticeOptionItem(
                                    option.getOptionKey(), option.getOptionText()))
                            .toList(),
                    resolveAnswer(question, options),
                    wrong.getLastAnswer(),
                    question.getAnalysis(),
                    nullSafe(wrong.getErrorCount()),
                    nullSafe(wrong.getCorrectedCount()),
                    nullSafe(wrong.getStatus()),
                    wrong.getLastWrongAt(),
                    subject == null ? null : subject.getName(),
                    stage == null ? null : stage.getName());
        }).toList();

        return PageResult.of(items, total, safePage, safePageSize);
    }

    /** P-W03 错题重做：按错题记录顺序选题，复用刷题组卷。 */
    public PracticeStartResponse redoStart(WrongRedoRequest request) {
        long studentId = currentStudentId();
        Map<Long, WrongQuestion> rows = wrongQuestionMapper.selectBatchIds(request.ids()).stream()
                .collect(Collectors.toMap(WrongQuestion::getId, Function.identity(), (a, b) -> a));
        Set<Long> questionIds = new LinkedHashSet<>();
        for (Long id : request.ids()) {
            WrongQuestion wrong = rows.get(id);
            if (wrong != null && Objects.equals(wrong.getStudentId(), studentId)
                    && wrong.getQuestionId() != null) {
                questionIds.add(wrong.getQuestionId());
            }
        }
        if (questionIds.isEmpty()) {
            throw new BizException(ErrorCode.WRONG_QUESTION_NOT_FOUND, "没有可重做的错题");
        }
        return practiceService.startWrongRedo(List.copyOf(questionIds));
    }

    /** P-W04 标记已掌握，幂等。 */
    public void markMastered(long id) {
        long studentId = currentStudentId();
        WrongQuestion wrong = requireWrong(id, studentId);
        if (nullSafe(wrong.getStatus()) == 2) {
            return;
        }
        wrongQuestionMapper.update(null, Wrappers.<WrongQuestion>lambdaUpdate()
                .eq(WrongQuestion::getId, id)
                .eq(WrongQuestion::getStudentId, studentId)
                .set(WrongQuestion::getStatus, 2));
    }

    /** P-W05 移除错题：逻辑删除，后续再次答错会恢复。 */
    public void remove(long id) {
        long studentId = currentStudentId();
        requireWrong(id, studentId);
        wrongQuestionMapper.deleteById(id);
    }

    private WrongQuestion requireWrong(long id, long studentId) {
        WrongQuestion wrong = wrongQuestionMapper.selectOne(Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getId, id)
                .eq(WrongQuestion::getStudentId, studentId));
        if (wrong == null) {
            throw new BizException(ErrorCode.WRONG_QUESTION_NOT_FOUND);
        }
        return wrong;
    }

    private Map<Long, Question> loadQuestions(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Map.of();
        }
        return questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, List<QuestionOption>> loadOptions(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Map.of();
        }
        return questionOptionMapper.selectList(Wrappers.<QuestionOption>lambdaQuery()
                        .in(QuestionOption::getQuestionId, questionIds)
                        .orderByAsc(QuestionOption::getSortOrder)
                        .orderByAsc(QuestionOption::getId)).stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));
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

    private static Comparator<WrongQuestion> recentComparator() {
        return Comparator
                .comparing(WrongQuestion::getLastWrongAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(WrongQuestion::getId, Comparator.reverseOrder());
    }

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

    private static void validateFilters(Integer status, Integer type, Integer days) {
        if (status != null && status != 1 && status != 2) {
            throw new BizException(ErrorCode.PARAM_INVALID, "错题状态不合法");
        }
        if (type != null && (type < 1 || type > 4)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "题型参数不合法");
        }
        if (days != null && days != 7 && days != 30) {
            throw new BizException(ErrorCode.PARAM_INVALID, "时间筛选仅支持 7 天或 30 天");
        }
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}

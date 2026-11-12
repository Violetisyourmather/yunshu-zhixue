package com.yunshu.zhixue.modules.question.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.question.dto.PortalQuestionDetail;
import com.yunshu.zhixue.modules.question.dto.PortalQuestionItem;
import com.yunshu.zhixue.modules.question.dto.PortalQuestionOptionItem;
import com.yunshu.zhixue.modules.question.dto.PortalStageItem;
import com.yunshu.zhixue.modules.question.dto.PortalSubjectItem;
import com.yunshu.zhixue.modules.question.dto.QuestionStatRow;
import com.yunshu.zhixue.modules.question.entity.Question;
import com.yunshu.zhixue.modules.question.entity.QuestionOption;
import com.yunshu.zhixue.modules.question.mapper.QuestionMapper;
import com.yunshu.zhixue.modules.question.mapper.QuestionOptionMapper;
import com.yunshu.zhixue.modules.student.entity.Student;
import com.yunshu.zhixue.modules.student.mapper.StudentMapper;
import com.yunshu.zhixue.modules.subject.dto.IdCountRow;
import com.yunshu.zhixue.modules.subject.entity.Stage;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.StageMapper;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学员端题库浏览（P-B01 ~ P-B04）。
 *
 * <p>仅展示已上架题目与启用阶段；游客可浏览题干与选项，
 * 登录后才返回答案、解析与本人错题本状态。</p>
 */
@Service
public class PortalQuestionService {

    private static final int PAGE_SIZE_DEFAULT = 10;
    private static final int PAGE_SIZE_MAX = 100;

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final StageMapper stageMapper;
    private final SubjectMapper subjectMapper;
    private final StudentMapper studentMapper;

    public PortalQuestionService(QuestionMapper questionMapper,
                                 QuestionOptionMapper questionOptionMapper,
                                 StageMapper stageMapper,
                                 SubjectMapper subjectMapper,
                                 StudentMapper studentMapper) {
        this.questionMapper = questionMapper;
        this.questionOptionMapper = questionOptionMapper;
        this.stageMapper = stageMapper;
        this.subjectMapper = subjectMapper;
        this.studentMapper = studentMapper;
    }

    /** P-B01 学科列表（公开）。 */
    public List<PortalSubjectItem> subjects() {
        List<Subject> subjects = subjectMapper.selectList(Wrappers.<Subject>lambdaQuery()
                .orderByAsc(Subject::getSortOrder)
                .orderByAsc(Subject::getId));
        if (subjects.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> stageCounts = stageMapper.selectList(Wrappers.<Stage>lambdaQuery()
                        .eq(Stage::getStatus, 1)).stream()
                .collect(Collectors.groupingBy(Stage::getSubjectId, Collectors.counting()));
        return subjects.stream()
                .map(subject -> new PortalSubjectItem(
                        subject.getId(),
                        subject.getName(),
                        subject.getIcon(),
                        subject.getDescription(),
                        stageCounts.getOrDefault(subject.getId(), 0L),
                        nullSafe(subject.getSortOrder())))
                .toList();
    }

    /** P-B02 阶段列表（公开，仅启用阶段与已上架题目数）。 */
    public List<PortalStageItem> stages(long subjectId) {
        if (subjectMapper.selectById(subjectId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学科不存在");
        }
        List<Stage> stages = stageMapper.selectList(Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getSubjectId, subjectId)
                .eq(Stage::getStatus, 1)
                .orderByAsc(Stage::getSortOrder)
                .orderByAsc(Stage::getId));
        Map<Long, Long> counts = toCountMap(questionMapper.countPublishedByStage());
        return stages.stream()
                .map(stage -> new PortalStageItem(
                        stage.getId(),
                        stage.getName(),
                        stage.getIcon(),
                        counts.getOrDefault(stage.getId(), 0L),
                        nullSafe(stage.getSortOrder())))
                .toList();
    }

    /** P-B03 题目列表（公开，不返回选项、答案与解析）。 */
    public PageResult<PortalQuestionItem> list(String keyword, Long subjectId, Long stageId,
                                               Integer type, Integer difficulty,
                                               long page, long pageSize) {
        validateFilter(subjectId, stageId, type, difficulty);
        long safePage = Math.max(page, 1);
        long safePageSize = pageSize <= 0
                ? PAGE_SIZE_DEFAULT
                : Math.min(pageSize, PAGE_SIZE_MAX);

        var query = Wrappers.<Question>lambdaQuery()
                .eq(Question::getStatus, QuestionService.STATUS_ONLINE);
        String stageCondition = "SELECT id FROM stage WHERE delete_flag = 0 AND status = 1";
        if (subjectId != null) {
            // Long 参数拼接安全，避免 MyBatis-Plus inSql 无法绑定参数的局限
            stageCondition += " AND subject_id = " + subjectId;
        }
        query.inSql(Question::getStageId, stageCondition);
        if (stageId != null) {
            query.eq(Question::getStageId, stageId);
        }
        if (type != null) {
            query.eq(Question::getType, type);
        }
        if (difficulty != null) {
            query.eq(Question::getDifficulty, difficulty);
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper.like(Question::getContent, value)
                    .or().like(Question::getTags, value));
        }
        query.orderByAsc(Question::getId);

        Page<Question> result = questionMapper.selectPage(new Page<>(safePage, safePageSize), query);
        return PageResult.of(toItems(result.getRecords()), result.getTotal(), safePage, safePageSize);
    }

    /** P-B04 题目详情，按登录态裁剪答案与解析。 */
    public PortalQuestionDetail detail(long id) {
        Question question = questionMapper.selectById(id);
        if (question == null || !Objects.equals(question.getStatus(), QuestionService.STATUS_ONLINE)) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        Stage stage = stageMapper.selectById(question.getStageId());
        if (stage == null || !Objects.equals(stage.getStatus(), 1)) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        Subject subject = subjectMapper.selectById(stage.getSubjectId());
        List<QuestionOption> options = questionOptionMapper.selectList(
                Wrappers.<QuestionOption>lambdaQuery()
                        .eq(QuestionOption::getQuestionId, id)
                        .orderByAsc(QuestionOption::getSortOrder)
                        .orderByAsc(QuestionOption::getId));

        Long userId = SecurityUtils.currentUserIdOrNull();
        boolean loggedIn = false;
        if (userId != null) {
            Student student = studentMapper.selectById(userId);
            if (student == null) {
                throw new BizException(ErrorCode.UNAUTHORIZED);
            }
            if (!student.enabled()) {
                throw new BizException(ErrorCode.STUDENT_DISABLED);
            }
            loggedIn = true;
        }

        String answer = loggedIn ? resolveAnswer(question, options) : null;
        String analysis = loggedIn ? question.getAnalysis() : null;
        boolean inWrongBook = loggedIn && questionMapper.countInWrongBook(userId, id) > 0;

        QuestionStatRow stat = loadStat(id);
        long done = statTotal(stat);
        long correct = statCorrect(stat);
        return new PortalQuestionDetail(
                question.getId(),
                nullSafe(question.getType()),
                nullSafe(question.getDifficulty()),
                question.getContent(),
                subject == null ? null : subject.getName(),
                stage.getName(),
                question.getTags(),
                options.stream()
                        .map(option -> new PortalQuestionOptionItem(
                                option.getOptionKey(), option.getOptionText()))
                        .toList(),
                answer,
                analysis,
                inWrongBook,
                done,
                accuracyRate(done, correct));
    }

    private List<PortalQuestionItem> toItems(List<Question> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }
        Set<Long> stageIds = questions.stream().map(Question::getStageId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Stage> stages = stageIds.isEmpty() ? Map.of()
                : stageMapper.selectBatchIds(stageIds).stream()
                .collect(Collectors.toMap(Stage::getId, Function.identity(), (a, b) -> a));
        Set<Long> subjectIds = stages.values().stream().map(Stage::getSubjectId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Subject> subjects = subjectIds.isEmpty() ? Map.of()
                : subjectMapper.selectBatchIds(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity(), (a, b) -> a));
        Map<Long, QuestionStatRow> stats = questionMapper.selectPracticeStats(
                        questions.stream().map(Question::getId).toList()).stream()
                .collect(Collectors.toMap(QuestionStatRow::getId, Function.identity(), (a, b) -> a));

        return questions.stream().map(question -> {
            Stage stage = stages.get(question.getStageId());
            Subject subject = stage == null ? null : subjects.get(stage.getSubjectId());
            QuestionStatRow stat = stats.get(question.getId());
            long done = statTotal(stat);
            long correct = statCorrect(stat);
            return new PortalQuestionItem(
                    question.getId(),
                    nullSafe(question.getType()),
                    nullSafe(question.getDifficulty()),
                    question.getContent(),
                    subject == null ? null : subject.getName(),
                    stage == null ? null : stage.getName(),
                    question.getTags(),
                    done,
                    accuracyRate(done, correct));
        }).toList();
    }

    /** 选择题返回正确选项键（如 A,C），判断 / 简答返回题库答案文本。 */
    private String resolveAnswer(Question question, List<QuestionOption> options) {
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

    private QuestionStatRow loadStat(Long questionId) {
        List<QuestionStatRow> rows = questionMapper.selectPracticeStats(List.of(questionId));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<Long, Long> toCountMap(List<IdCountRow> rows) {
        return rows.stream().collect(Collectors.toMap(IdCountRow::getId,
                row -> nullSafe(row.getTotal()), (a, b) -> a));
    }

    private static void validateFilter(Long subjectId, Long stageId, Integer type, Integer difficulty) {
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

    private static long statTotal(QuestionStatRow stat) {
        return stat == null || stat.getTotal() == null ? 0L : stat.getTotal();
    }

    private static long statCorrect(QuestionStatRow stat) {
        return stat == null || stat.getCorrect() == null ? 0L : stat.getCorrect();
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

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}

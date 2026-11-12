package com.yunshu.zhixue.modules.question.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.question.dto.QuestionAuditRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionBatchDeleteRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionBatchResult;
import com.yunshu.zhixue.modules.question.dto.QuestionBatchStatusRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionDetail;
import com.yunshu.zhixue.modules.question.dto.QuestionItem;
import com.yunshu.zhixue.modules.question.dto.QuestionOptionItem;
import com.yunshu.zhixue.modules.question.dto.QuestionOptionRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionStageGroup;
import com.yunshu.zhixue.modules.question.dto.QuestionStageOption;
import com.yunshu.zhixue.modules.question.dto.QuestionStatRow;
import com.yunshu.zhixue.modules.question.dto.QuestionStats;
import com.yunshu.zhixue.modules.question.entity.Question;
import com.yunshu.zhixue.modules.question.entity.QuestionOption;
import com.yunshu.zhixue.modules.question.mapper.QuestionMapper;
import com.yunshu.zhixue.modules.question.mapper.QuestionOptionMapper;
import com.yunshu.zhixue.modules.subject.entity.Stage;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.StageMapper;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 题库管理（A-Q01 ~ A-Q11）。
 *
 * <p>状态机（04 附录 D）：新增 → 待审核；通过 → 已上架；不通过 → 已驳回；
 * 已驳回可重新提交回待审核；已上架可下架，已下架重新上架后回到待审核。</p>
 */
@Service
public class QuestionService {

    public static final int TYPE_SINGLE = 1;
    public static final int TYPE_MULTI = 2;
    public static final int TYPE_JUDGE = 3;
    public static final int TYPE_SHORT = 4;

    public static final int STATUS_ONLINE = 1;
    public static final int STATUS_OFFLINE = 2;
    public static final int STATUS_PENDING = 3;
    public static final int STATUS_REJECTED = 5;

    private static final String TARGET = "题目";
    private static final int PAGE_SIZE_MAX = 100;
    private static final int MAX_OPTIONS = 6;

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final StageMapper stageMapper;
    private final SubjectMapper subjectMapper;
    private final OperationLogRecorder operationLogRecorder;

    public QuestionService(QuestionMapper questionMapper,
                           QuestionOptionMapper questionOptionMapper,
                           StageMapper stageMapper,
                           SubjectMapper subjectMapper,
                           OperationLogRecorder operationLogRecorder) {
        this.questionMapper = questionMapper;
        this.questionOptionMapper = questionOptionMapper;
        this.stageMapper = stageMapper;
        this.subjectMapper = subjectMapper;
        this.operationLogRecorder = operationLogRecorder;
    }

    /** A-Q01 统计卡。 */
    public QuestionStats stats() {
        return new QuestionStats(
                questionMapper.selectCount(null),
                questionMapper.selectCount(Wrappers.<Question>lambdaQuery()
                        .eq(Question::getStatus, STATUS_ONLINE)),
                questionMapper.selectCount(Wrappers.<Question>lambdaQuery()
                        .eq(Question::getStatus, STATUS_PENDING)),
                questionMapper.selectCount(Wrappers.<Question>lambdaQuery()
                        .eq(Question::getType, TYPE_SINGLE)),
                questionMapper.selectCount(Wrappers.<Question>lambdaQuery()
                        .eq(Question::getType, TYPE_MULTI)),
                questionMapper.selectCount(Wrappers.<Question>lambdaQuery()
                        .eq(Question::getType, TYPE_JUDGE)),
                questionMapper.selectCount(Wrappers.<Question>lambdaQuery()
                        .eq(Question::getType, TYPE_SHORT)));
    }

    /** 按学科分组的阶段下拉（筛选、表单、导入目标学科共用）。 */
    public List<QuestionStageGroup> stageGroups() {
        List<Subject> subjects = subjectMapper.selectList(Wrappers.<Subject>lambdaQuery()
                .orderByAsc(Subject::getSortOrder)
                .orderByAsc(Subject::getId));
        List<Stage> stages = stageMapper.selectList(Wrappers.<Stage>lambdaQuery()
                .orderByAsc(Stage::getSortOrder)
                .orderByAsc(Stage::getId));
        Map<Long, List<Stage>> bySubject = stages.stream()
                .collect(Collectors.groupingBy(Stage::getSubjectId,
                        LinkedHashMap::new, Collectors.toList()));
        return subjects.stream()
                .map(subject -> new QuestionStageGroup(subject.getId(), subject.getName(),
                        bySubject.getOrDefault(subject.getId(), List.of()).stream()
                                .map(stage -> new QuestionStageOption(stage.getId(), stage.getName(),
                                        nullSafe(stage.getStatus())))
                                .toList()))
                .toList();
    }

    /** A-Q02 分页列表（按 ID 正序 / 倒序）。 */
    public PageResult<QuestionItem> list(String keyword, Long subjectId, Long stageId,
                                         Integer type, Integer difficulty, Integer status,
                                         String sort, long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), PAGE_SIZE_MAX);

        var query = Wrappers.<Question>lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(w -> w.like(Question::getContent, value).or().like(Question::getTags, value));
        }
        if (subjectId != null) {
            long targetSubjectId = subjectId;
            query.inSql(Question::getStageId,
                    "SELECT id FROM stage WHERE subject_id = " + targetSubjectId + " AND delete_flag = 0");
        }
        if (stageId != null) {
            query.eq(Question::getStageId, stageId);
        }
        if (type != null) {
            query.eq(Question::getType, type);
        }
        if (difficulty != null) {
            query.eq(Question::getDifficulty, difficulty);
        }
        if (status != null) {
            query.eq(Question::getStatus, status);
        }
        if ("desc".equalsIgnoreCase(sort)) {
            query.orderByDesc(Question::getId);
        } else {
            query.orderByAsc(Question::getId);
        }

        Page<Question> result = questionMapper.selectPage(new Page<>(safePage, safePageSize), query);
        return PageResult.of(toItems(result.getRecords()), result.getTotal(), safePage, safePageSize);
    }

    /** A-Q03 详情。 */
    public QuestionDetail detail(Long id) {
        Question question = requireQuestion(id);
        Stage stage = stageMapper.selectById(question.getStageId());
        Subject subject = stage == null ? null : subjectMapper.selectById(stage.getSubjectId());
        List<QuestionOption> options = questionOptionMapper.selectList(
                Wrappers.<QuestionOption>lambdaQuery()
                        .eq(QuestionOption::getQuestionId, id)
                        .orderByAsc(QuestionOption::getSortOrder)
                        .orderByAsc(QuestionOption::getId));
        QuestionStatRow stat = loadStat(id);
        return new QuestionDetail(question.getId(), question.getStageId(),
                stage == null ? null : stage.getName(),
                stage == null ? 0L : nullSafe(stage.getSubjectId()),
                subject == null ? null : subject.getName(),
                nullSafe(question.getType()), nullSafe(question.getDifficulty()),
                question.getContent(), question.getAnswer(), question.getAnalysis(), question.getTags(),
                nullSafe(question.getStatus()), question.getRejectReason(), question.getCreatedAt(),
                options.stream().map(option -> new QuestionOptionItem(option.getOptionKey(),
                        option.getOptionText(), Integer.valueOf(1).equals(option.getIsCorrect()))).toList(),
                statTotal(stat), statCorrect(stat), accuracyRate(statTotal(stat), statCorrect(stat)));
    }

    /** A-Q04 新增题目（初始待审核）。 */
    @Transactional
    public Long create(QuestionRequest request) {
        ValidatedContent content = validateContent(request);
        Question question = new Question();
        question.setStageId(request.stageId());
        question.setType(request.type());
        question.setDifficulty(request.difficulty());
        question.setContent(request.content().trim());
        question.setAnswer(content.answer());
        question.setAnalysis(trimToNull(request.analysis()));
        question.setTags(trimToNull(request.tags()));
        question.setStatus(STATUS_PENDING);
        questionMapper.insert(question);
        insertOptions(question.getId(), content.options());
        operationLogRecorder.record(OperationActions.CREATE, TARGET, truncate(question.getContent()));
        return question.getId();
    }

    /** A-Q05 编辑题目：已驳回编辑后回到待审核，其余保持原状态。 */
    @Transactional
    public void update(Long id, QuestionRequest request) {
        Question existing = requireQuestion(id);
        ValidatedContent content = validateContent(request);
        int status = existing.getStatus() != null && existing.getStatus() == STATUS_REJECTED
                ? STATUS_PENDING : nullSafe(existing.getStatus());
        String rejectReason = status == STATUS_PENDING ? null : existing.getRejectReason();

        questionMapper.update(null, Wrappers.<Question>lambdaUpdate()
                .eq(Question::getId, id)
                .set(Question::getStageId, request.stageId())
                .set(Question::getType, request.type())
                .set(Question::getDifficulty, request.difficulty())
                .set(Question::getContent, request.content().trim())
                .set(Question::getAnswer, content.answer())
                .set(Question::getAnalysis, trimToNull(request.analysis()))
                .set(Question::getTags, trimToNull(request.tags()))
                .set(Question::getStatus, status)
                .set(Question::getRejectReason, rejectReason));
        questionOptionMapper.deleteByQuestionId(id);
        insertOptions(id, content.options());
        operationLogRecorder.record(OperationActions.UPDATE, TARGET, truncate(request.content()));
    }

    /** A-Q07 审核。 */
    @Transactional
    public void audit(Long id, QuestionAuditRequest request) {
        Question question = requireQuestion(id);
        if (nullSafe(question.getStatus()) != STATUS_PENDING) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_DENIED, "仅待审核题目可执行审核");
        }
        int status;
        String rejectReason;
        if (Boolean.TRUE.equals(request.pass())) {
            status = STATUS_ONLINE;
            rejectReason = null;
        } else {
            rejectReason = trimToNull(request.rejectReason());
            if (rejectReason == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "请填写驳回原因");
            }
            status = STATUS_REJECTED;
        }
        questionMapper.update(null, Wrappers.<Question>lambdaUpdate()
                .eq(Question::getId, id)
                .set(Question::getStatus, status)
                .set(Question::getRejectReason, rejectReason));
        operationLogRecorder.record(OperationActions.UPDATE, TARGET,
                (status == STATUS_ONLINE ? "审核通过：" : "审核驳回：") + truncate(question.getContent()));
    }

    /** A-Q08 上架 / 下架。 */
    @Transactional
    public void updateStatus(Long id, String action) {
        Question question = requireQuestion(id);
        int current = nullSafe(question.getStatus());
        int target;
        if ("off".equals(action)) {
            if (current != STATUS_ONLINE) {
                throw new BizException(ErrorCode.STATUS_TRANSITION_DENIED, "仅已上架题目可下架");
            }
            target = STATUS_OFFLINE;
        } else {
            if (current != STATUS_OFFLINE) {
                throw new BizException(ErrorCode.STATUS_TRANSITION_DENIED, "仅已下架题目可重新上架");
            }
            target = STATUS_PENDING;
        }
        questionMapper.update(null, Wrappers.<Question>lambdaUpdate()
                .eq(Question::getId, id)
                .set(Question::getStatus, target)
                .set(Question::getRejectReason, null));
        operationLogRecorder.record(OperationActions.STATUS, TARGET,
                (target == STATUS_PENDING ? "重新提交审核：" : "下架：") + truncate(question.getContent()));
    }

    /** A-Q09 已驳回 → 重新提交 → 待审核。 */
    @Transactional
    public void resubmit(Long id) {
        Question question = requireQuestion(id);
        if (nullSafe(question.getStatus()) != STATUS_REJECTED) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_DENIED, "仅已驳回题目可重新提交");
        }
        questionMapper.update(null, Wrappers.<Question>lambdaUpdate()
                .eq(Question::getId, id)
                .set(Question::getStatus, STATUS_PENDING)
                .set(Question::getRejectReason, null));
        operationLogRecorder.record(OperationActions.UPDATE, TARGET,
                "重新提交审核：" + truncate(question.getContent()));
    }

    /** A-Q10 批量上架 / 下架：仅处理状态允许的题目，返回更新与被跳过数量。 */
    @Transactional
    public QuestionBatchResult batchStatus(QuestionBatchStatusRequest request) {
        List<Question> questions = questionMapper.selectBatchIds(request.ids());
        if (questions.isEmpty()) {
            return new QuestionBatchResult(0, 0);
        }
        int target = "off".equals(request.action()) ? STATUS_OFFLINE : STATUS_PENDING;
        List<Long> updateIds = questions.stream()
                .filter(question -> "off".equals(request.action())
                        ? nullSafe(question.getStatus()) == STATUS_ONLINE
                        : nullSafe(question.getStatus()) == STATUS_OFFLINE)
                .map(Question::getId)
                .toList();
        if (!updateIds.isEmpty()) {
            questionMapper.update(null, Wrappers.<Question>lambdaUpdate()
                    .in(Question::getId, updateIds)
                    .set(Question::getStatus, target)
                    .set(Question::getRejectReason, null));
        }
        operationLogRecorder.record(OperationActions.STATUS, TARGET,
                ("off".equals(request.action()) ? "批量下架 " : "批量重新提交 ")
                        + updateIds.size() + " 道题");
        return new QuestionBatchResult(updateIds.size(), questions.size() - updateIds.size());
    }

    /** A-Q06 删除题目：被练习/错题引用 → 30007。 */
    @Transactional
    public void delete(Long id) {
        Question question = requireQuestion(id);
        ensureNotReferenced(List.of(id));
        questionOptionMapper.deleteByQuestionId(id);
        questionMapper.deleteById(id);
        operationLogRecorder.record(OperationActions.DELETE, TARGET, truncate(question.getContent()));
    }

    /** A-Q11 批量删除。 */
    @Transactional
    public void batchDelete(QuestionBatchDeleteRequest request) {
        List<Question> questions = questionMapper.selectBatchIds(request.ids());
        if (questions.isEmpty()) {
            return;
        }
        ensureNotReferenced(questions.stream().map(Question::getId).toList());
        for (Question question : questions) {
            questionOptionMapper.deleteByQuestionId(question.getId());
        }
        questionMapper.deleteBatchIds(questions.stream().map(Question::getId).toList());
        operationLogRecorder.record(OperationActions.DELETE, TARGET,
                "批量删除 " + questions.size() + " 道题");
    }

    private List<QuestionItem> toItems(List<Question> questions) {
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
            return new QuestionItem(question.getId(), question.getContent(),
                    question.getStageId(), stage == null ? null : stage.getName(),
                    stage == null ? 0L : nullSafe(stage.getSubjectId()),
                    subject == null ? null : subject.getName(),
                    nullSafe(question.getType()), nullSafe(question.getDifficulty()),
                    nullSafe(question.getStatus()), question.getRejectReason(), question.getTags(),
                    question.getCreatedAt(), done, correct, accuracyRate(done, correct));
        }).toList();
    }

    private void ensureNotReferenced(Collection<Long> questionIds) {
        long referenced = 0;
        for (Long questionId : questionIds) {
            if (questionMapper.countPracticeReferences(questionId) > 0
                    || questionMapper.countWrongReferences(questionId) > 0) {
                referenced++;
            }
        }
        if (referenced > 0) {
            throw new BizException(ErrorCode.RELATED_CONTENT_EXISTS,
                    "有 " + referenced + " 道题已被练习或错题记录引用，不可删除");
        }
    }

    private Question requireQuestion(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        return question;
    }

    /** 校验题型专属字段并构造待落库选项。 */
    private ValidatedContent validateContent(QuestionRequest request) {
        Stage stage = stageMapper.selectById(request.stageId());
        if (stage == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "阶段不存在");
        }
        int type = request.type();
        if (type == TYPE_SINGLE || type == TYPE_MULTI) {
            List<QuestionOptionRequest> rawOptions = request.options();
            if (rawOptions == null || rawOptions.size() < 2) {
                throw new BizException(ErrorCode.PARAM_INVALID, "选择题至少需要 2 个选项");
            }
            if (rawOptions.size() > MAX_OPTIONS) {
                throw new BizException(ErrorCode.PARAM_INVALID, "选项最多 " + MAX_OPTIONS + " 个");
            }
            LinkedHashMap<String, String> byKey = new LinkedHashMap<>();
            for (int index = 0; index < rawOptions.size(); index++) {
                QuestionOptionRequest raw = rawOptions.get(index);
                String text = trimToNull(raw.text());
                if (text == null) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "选项内容不能为空");
                }
                String key = StringUtils.hasText(raw.key())
                        ? raw.key().trim().toUpperCase(Locale.ROOT)
                        : String.valueOf((char) ('A' + index));
                if (!key.matches("[A-Z]")) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "选项标识不合法：" + key);
                }
                if (byKey.containsKey(key)) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "选项标识重复：" + key);
                }
                byKey.put(key, text);
            }
            List<String> answerKeys = request.answerKeys() == null ? List.of()
                    : request.answerKeys().stream()
                    .filter(StringUtils::hasText)
                    .map(key -> key.trim().toUpperCase(Locale.ROOT))
                    .distinct()
                    .toList();
            if (answerKeys.isEmpty()) {
                throw new BizException(ErrorCode.PARAM_INVALID, "请设置正确答案");
            }
            if (type == TYPE_SINGLE && answerKeys.size() != 1) {
                throw new BizException(ErrorCode.PARAM_INVALID, "单选题只能有一个正确答案");
            }
            for (String key : answerKeys) {
                if (!byKey.containsKey(key)) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "正确答案「" + key + "」没有对应选项");
                }
            }
            List<OptionData> options = new ArrayList<>();
            int sortOrder = 1;
            for (Map.Entry<String, String> entry : byKey.entrySet()) {
                options.add(new OptionData(entry.getKey(), entry.getValue(),
                        answerKeys.contains(entry.getKey()), sortOrder++));
            }
            return new ValidatedContent(stage, null, options);
        }
        if (type == TYPE_JUDGE) {
            String answer = normalizeJudgeAnswer(request.answer());
            if (answer == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "判断题答案必须为「正确」或「错误」");
            }
            return new ValidatedContent(stage, answer, List.of());
        }
        if (type == TYPE_SHORT) {
            String answer = trimToNull(request.answer());
            if (answer == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "简答题必须填写参考答案");
            }
            return new ValidatedContent(stage, answer, List.of());
        }
        throw new BizException(ErrorCode.PARAM_INVALID, "题型不合法");
    }

    private void insertOptions(Long questionId, List<OptionData> options) {
        for (OptionData option : options) {
            QuestionOption entity = new QuestionOption();
            entity.setQuestionId(questionId);
            entity.setOptionKey(option.key());
            entity.setOptionText(option.text());
            entity.setIsCorrect(option.correct() ? 1 : 0);
            entity.setSortOrder(option.sortOrder());
            questionOptionMapper.insert(entity);
        }
    }

    static String normalizeJudgeAnswer(String raw) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (value.contains("错") || value.contains("不对") || "×".equals(value)
                || "f".equals(normalized) || "false".equals(normalized) || "否".equals(value)) {
            return "错误";
        }
        if (value.contains("正确") || "对".equals(value) || "√".equals(value)
                || "t".equals(normalized) || "true".equals(normalized) || "是".equals(value)) {
            return "正确";
        }
        return null;
    }

    private QuestionStatRow loadStat(Long questionId) {
        List<QuestionStatRow> rows = questionMapper.selectPracticeStats(List.of(questionId));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static long statTotal(QuestionStatRow stat) {
        return stat == null || stat.getTotal() == null ? 0L : stat.getTotal();
    }

    private static long statCorrect(QuestionStatRow stat) {
        return stat == null || stat.getCorrect() == null ? 0L : stat.getCorrect();
    }

    static double accuracyRate(long done, long correct) {
        if (done <= 0) {
            return 0.0;
        }
        return Math.round(correct * 1000.0 / done) / 10.0;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 30 ? value : value.substring(0, 30) + "…";
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private record OptionData(String key, String text, boolean correct, int sortOrder) {
    }

    private record ValidatedContent(Stage stage, String answer, List<OptionData> options) {
    }
}

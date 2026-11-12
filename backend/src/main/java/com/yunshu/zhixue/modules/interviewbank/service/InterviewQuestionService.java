package com.yunshu.zhixue.modules.interviewbank.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionCreateRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionDetail;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionItem;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionUpdateRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewSearchResultItem;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewStats;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewSubjectOption;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTopicItem;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTopicRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.TopicCountRow;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewQuestion;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewTopic;
import com.yunshu.zhixue.modules.interviewbank.mapper.InterviewQuestionMapper;
import com.yunshu.zhixue.modules.interviewbank.mapper.InterviewTopicMapper;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 面试题库管理（A-I01 ~ A-I05、A-I10 ~ A-I13）。
 *
 * <p>面试题按方向（= 学科）归属；题材存主题名称字符串，主题词表用于筛选与维护。
 * 题干与参考答案只读，仅主题与难度可编辑。</p>
 */
@Service
public class InterviewQuestionService {

    private static final int PAGE_SIZE_MAX = 100;
    private static final String TARGET_QUESTION = "面试题";
    private static final String TARGET_TOPIC = "面试题主题";

    private final InterviewQuestionMapper questionMapper;
    private final InterviewTopicMapper topicMapper;
    private final SubjectMapper subjectMapper;
    private final OperationLogRecorder operationLogRecorder;

    public InterviewQuestionService(InterviewQuestionMapper questionMapper,
                                    InterviewTopicMapper topicMapper,
                                    SubjectMapper subjectMapper,
                                    OperationLogRecorder operationLogRecorder) {
        this.questionMapper = questionMapper;
        this.topicMapper = topicMapper;
        this.subjectMapper = subjectMapper;
        this.operationLogRecorder = operationLogRecorder;
    }

    /** 面试题库统计卡。 */
    public InterviewStats stats() {
        long total = questionMapper.selectCount(null);
        long unclassified = questionMapper.selectCount(Wrappers.<InterviewQuestion>lambdaQuery()
                .and(w -> w.isNull(InterviewQuestion::getTopic)
                        .or().eq(InterviewQuestion::getTopic, "")));
        LocalDateTime todayStartUtc = LocalDate.now(AppConstants.BUSINESS_ZONE)
                .atStartOfDay(AppConstants.BUSINESS_ZONE)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        long todayAdded = questionMapper.selectCount(Wrappers.<InterviewQuestion>lambdaQuery()
                .ge(InterviewQuestion::getCreatedAt, todayStartUtc));
        return new InterviewStats(total, subjectMapper.selectCount(null), unclassified, todayAdded);
    }

    /** 方向（学科）下拉。 */
    public List<InterviewSubjectOption> subjectOptions() {
        return subjectMapper.selectList(Wrappers.<Subject>lambdaQuery()
                        .orderByAsc(Subject::getSortOrder)
                        .orderByAsc(Subject::getId)).stream()
                .map(subject -> new InterviewSubjectOption(subject.getId(), subject.getName()))
                .toList();
    }

    /** A-I01 列表。 */
    public PageResult<InterviewQuestionItem> list(String keyword, Long subjectId, Long topicId,
                                                  Integer difficulty, boolean unclassified,
                                                  long page, long pageSize) {
        if (topicId != null && unclassified) {
            throw new BizException(ErrorCode.PARAM_INVALID, "主题筛选与只看未分类不能同时使用");
        }
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), PAGE_SIZE_MAX);

        var query = Wrappers.<InterviewQuestion>lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            query.like(InterviewQuestion::getContent, keyword.trim());
        }
        if (subjectId != null) {
            query.eq(InterviewQuestion::getSubjectId, subjectId);
        }
        if (topicId != null) {
            InterviewTopic topic = requireTopic(topicId);
            query.eq(InterviewQuestion::getTopic, topic.getName());
        }
        if (unclassified) {
            query.and(w -> w.isNull(InterviewQuestion::getTopic)
                    .or().eq(InterviewQuestion::getTopic, ""));
        }
        if (difficulty != null) {
            query.eq(InterviewQuestion::getDifficulty, difficulty);
        }
        query.orderByDesc(InterviewQuestion::getId);

        Page<InterviewQuestion> result = questionMapper.selectPage(
                new Page<>(safePage, safePageSize), query);
        Map<Long, Subject> subjects = loadSubjects(result.getRecords().stream()
                .map(InterviewQuestion::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        List<InterviewQuestionItem> items = result.getRecords().stream()
                .map(question -> {
                    Subject subject = subjects.get(question.getSubjectId());
                    return new InterviewQuestionItem(question.getId(), question.getContent(),
                            nullSafe(question.getSubjectId()),
                            subject == null ? null : subject.getName(),
                            question.getTopic(), nullSafe(question.getDifficulty()),
                            question.getTags(), nullSafe(question.getSourceType()),
                            question.getSourceDocName(), question.getSourceSeq(),
                            nullSafe(question.getStatus()), question.getCreatedAt());
                })
                .toList();
        return PageResult.of(items, result.getTotal(), safePage, safePageSize);
    }

    /** A-I02 详情。 */
    public InterviewQuestionDetail detail(Long id) {
        InterviewQuestion question = requireQuestion(id);
        Subject subject = subjectMapper.selectById(question.getSubjectId());
        return new InterviewQuestionDetail(question.getId(), question.getContent(),
                question.getReferenceAnswer(), nullSafe(question.getSubjectId()),
                subject == null ? null : subject.getName(), question.getTopic(),
                nullSafe(question.getDifficulty()), question.getTags(),
                nullSafe(question.getSourceType()), question.getSourceDocName(),
                question.getSourceSeq(), nullSafe(question.getStatus()), question.getCreatedAt());
    }

    /** A-I03 手工录入面试题。 */
    @Transactional
    public Long create(InterviewQuestionCreateRequest request) {
        requireSubject(request.subjectId());
        String topic = trimToNull(request.topic());
        if (topic != null) {
            ensureTopic(topic);
        }
        InterviewQuestion question = new InterviewQuestion();
        question.setSubjectId(request.subjectId());
        question.setContent(request.content().trim());
        question.setReferenceAnswer(trimToNull(request.referenceAnswer()));
        question.setTopic(topic);
        question.setDifficulty(request.difficulty() == null ? 2 : request.difficulty());
        question.setTags(trimToNull(request.tags()));
        question.setSourceType(2);
        question.setStatus(1);
        questionMapper.insert(question);
        operationLogRecorder.record(OperationActions.CREATE, TARGET_QUESTION,
                truncate(question.getContent()));
        return question.getId();
    }

    /** A-I04 编辑：仅主题 + 难度。 */
    @Transactional
    public void update(Long id, InterviewQuestionUpdateRequest request) {
        InterviewQuestion question = requireQuestion(id);
        String topic = trimToNull(request.topic());
        if (topic != null) {
            ensureTopic(topic);
        }
        int difficulty = request.difficulty() == null
                ? nullSafe(question.getDifficulty()) : request.difficulty();
        questionMapper.update(null, Wrappers.<InterviewQuestion>lambdaUpdate()
                .eq(InterviewQuestion::getId, id)
                .set(InterviewQuestion::getTopic, topic)
                .set(InterviewQuestion::getDifficulty, difficulty));
        operationLogRecorder.record(OperationActions.UPDATE, TARGET_QUESTION,
                truncate(question.getContent()));
    }

    /** A-I05 删除（逻辑删除；按来源风险提示由前端给出）。 */
    @Transactional
    public void delete(Long id) {
        InterviewQuestion question = requireQuestion(id);
        questionMapper.deleteById(id);
        operationLogRecorder.record(OperationActions.DELETE, TARGET_QUESTION,
                truncate(question.getContent()));
    }

    /** A-I10 主题词表（含 0 题主题）。 */
    public List<InterviewTopicItem> topics() {
        List<InterviewTopic> topics = topicMapper.selectList(Wrappers.<InterviewTopic>lambdaQuery()
                .orderByAsc(InterviewTopic::getSortOrder)
                .orderByAsc(InterviewTopic::getId));
        Map<Long, Subject> subjects = loadSubjects(topics.stream()
                .map(InterviewTopic::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<String, Long> counts = questionMapper.countByTopic().stream()
                .collect(Collectors.toMap(TopicCountRow::getTopic,
                        row -> row.getTotal() == null ? 0L : row.getTotal(), (a, b) -> a));
        return topics.stream().map(topic -> {
            Subject subject = topic.getSubjectId() == null ? null : subjects.get(topic.getSubjectId());
            return new InterviewTopicItem(topic.getId(), topic.getName(), topic.getSubjectId(),
                    subject == null ? null : subject.getName(),
                    counts.getOrDefault(topic.getName(), 0L), nullSafe(topic.getSortOrder()));
        }).toList();
    }

    /** A-I11 新增主题。 */
    @Transactional
    public Long createTopic(InterviewTopicRequest request) {
        String name = request.name().trim();
        if (request.subjectId() != null) {
            requireSubject(request.subjectId());
        }
        ensureTopicNameUnique(name, null);
        InterviewTopic topic = new InterviewTopic();
        topic.setName(name);
        topic.setSubjectId(request.subjectId());
        topic.setSortOrder(nextTopicSortOrder());
        topicMapper.insert(topic);
        operationLogRecorder.record(OperationActions.CREATE, TARGET_TOPIC, name);
        return topic.getId();
    }

    /** A-I12 编辑主题：重命名时同步更新题目上的主题字符串。 */
    @Transactional
    public void updateTopic(Long id, InterviewTopicRequest request) {
        InterviewTopic topic = requireTopic(id);
        String name = request.name().trim();
        if (request.subjectId() != null) {
            requireSubject(request.subjectId());
        }
        ensureTopicNameUnique(name, id);
        String oldName = topic.getName();
        if (!oldName.equals(name)) {
            questionMapper.update(null, Wrappers.<InterviewQuestion>lambdaUpdate()
                    .eq(InterviewQuestion::getTopic, oldName)
                    .set(InterviewQuestion::getTopic, name));
        }
        topicMapper.update(null, Wrappers.<InterviewTopic>lambdaUpdate()
                .eq(InterviewTopic::getId, id)
                .set(InterviewTopic::getName, name)
                .set(InterviewTopic::getSubjectId, request.subjectId()));
        operationLogRecorder.record(OperationActions.UPDATE, TARGET_TOPIC, name);
    }

    /** A-I13 删除主题：下有题目 → 30007。 */
    @Transactional
    public void deleteTopic(Long id) {
        InterviewTopic topic = requireTopic(id);
        long questionCount = questionMapper.selectCount(Wrappers.<InterviewQuestion>lambdaQuery()
                .eq(InterviewQuestion::getTopic, topic.getName()));
        if (questionCount > 0) {
            throw new BizException(ErrorCode.RELATED_CONTENT_EXISTS,
                    "该主题下有 " + questionCount + " 道题，不可删除");
        }
        topicMapper.deleteById(id);
        operationLogRecorder.record(OperationActions.DELETE, TARGET_TOPIC, topic.getName());
    }

    /**
     * 确保主题词表中存在该名称的主题；不存在时创建全局主题。
     *
     * <p>题目表只存主题名称，因此按名称复用，避免导入/录入产生重复主题。</p>
     */
    @Transactional
    public void ensureTopic(String name) {
        String topicName = name.trim();
        InterviewTopic existing = topicMapper.selectOne(Wrappers.<InterviewTopic>lambdaQuery()
                .eq(InterviewTopic::getName, topicName)
                .last("LIMIT 1"));
        if (existing != null) {
            return;
        }
        InterviewTopic topic = new InterviewTopic();
        topic.setName(topicName);
        topic.setSubjectId(null);
        topic.setSortOrder(nextTopicSortOrder());
        topicMapper.insert(topic);
    }

    private void ensureTopicNameUnique(String name, Long excludeId) {
        var query = Wrappers.<InterviewTopic>lambdaQuery().eq(InterviewTopic::getName, name);
        if (excludeId != null) {
            query.ne(InterviewTopic::getId, excludeId);
        }
        if (topicMapper.selectCount(query) > 0) {
            throw new BizException(ErrorCode.NAME_EXISTS, "主题名称已存在");
        }
    }

    private int nextTopicSortOrder() {
        return topicMapper.selectList(null).stream()
                .mapToInt(topic -> nullSafe(topic.getSortOrder()))
                .max()
                .orElse(0) + 1;
    }

    private InterviewQuestion requireQuestion(Long id) {
        InterviewQuestion question = questionMapper.selectById(id);
        if (question == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "面试题不存在");
        }
        return question;
    }

    private InterviewTopic requireTopic(Long id) {
        InterviewTopic topic = topicMapper.selectById(id);
        if (topic == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "主题不存在");
        }
        return topic;
    }

    private void requireSubject(Long subjectId) {
        if (subjectMapper.selectById(subjectId) == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "所属方向不存在");
        }
    }

    private Map<Long, Subject> loadSubjects(Set<Long> subjectIds) {
        if (subjectIds.isEmpty()) {
            return Map.of();
        }
        return subjectMapper.selectBatchIds(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity(), (a, b) -> a));
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

    /**
     * A-I09 检索测试：按题干/参考答案模糊匹配，返回命中题目与来源。
     *
     * <p>用于验证 Markdown 解析与导入结果；最多返回 10 条。</p>
     */
    public List<InterviewSearchResultItem> searchTest(String keyword) {
        String value = keyword == null ? "" : keyword.trim();
        if (value.isEmpty()) {
            return List.of();
        }
        List<InterviewQuestion> questions = questionMapper.selectList(
                Wrappers.<InterviewQuestion>lambdaQuery()
                        .and(wrapper -> wrapper.like(InterviewQuestion::getContent, value)
                                .or().like(InterviewQuestion::getReferenceAnswer, value))
                        .orderByDesc(InterviewQuestion::getId)
                        .last("LIMIT 10"));
        if (questions.isEmpty()) {
            return List.of();
        }
        Set<Long> subjectIds = questions.stream()
                .map(InterviewQuestion::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> subjectNames = subjectIds.isEmpty()
                ? Map.of()
                : subjectMapper.selectBatchIds(subjectIds).stream()
                        .collect(Collectors.toMap(Subject::getId, Subject::getName, (a, b) -> a));
        return questions.stream()
                .map(question -> new InterviewSearchResultItem(
                        nullSafe(question.getId()),
                        question.getContent(),
                        question.getReferenceAnswer(),
                        nullSafe(question.getSubjectId()),
                        subjectNames.get(question.getSubjectId()),
                        question.getTopic(),
                        nullSafe(question.getDifficulty()),
                        question.getSourceDocName(),
                        question.getSourceSeq()))
                .toList();
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}

package com.yunshu.zhixue.modules.subject.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.subject.dto.IdCountRow;
import com.yunshu.zhixue.modules.subject.dto.SortRequest;
import com.yunshu.zhixue.modules.subject.dto.StageItem;
import com.yunshu.zhixue.modules.subject.dto.StageRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectCard;
import com.yunshu.zhixue.modules.subject.dto.SubjectRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectStats;
import com.yunshu.zhixue.modules.subject.entity.Stage;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.StageMapper;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogRecorder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 学科与阶段（A-S01 ~ A-S11）。
 *
 * <p>删除保护（03 §6、需求 4.3.1）：学科或阶段存在下级内容时拒绝删除（30007），
 * 由前端按「有阶段与题目 / 仅阶段 / 无阶段」三种情况给出提示。</p>
 */
@Service
public class SubjectService {

    private static final String TARGET_SUBJECT = "学科";
    private static final String TARGET_STAGE = "阶段";
    private static final String MOVE_UP = "up";

    private final SubjectMapper subjectMapper;
    private final StageMapper stageMapper;
    private final OperationLogRecorder operationLogRecorder;

    public SubjectService(SubjectMapper subjectMapper,
                          StageMapper stageMapper,
                          OperationLogRecorder operationLogRecorder) {
        this.subjectMapper = subjectMapper;
        this.stageMapper = stageMapper;
        this.operationLogRecorder = operationLogRecorder;
    }

    /** A-S01 统计卡。 */
    public SubjectStats stats() {
        List<Subject> subjects = subjectMapper.selectList(null);
        List<Stage> stages = stageMapper.selectList(null);
        Set<Long> subjectIds = subjects.stream().map(Subject::getId).collect(Collectors.toSet());
        long withStages = stages.stream()
                .map(Stage::getSubjectId)
                .filter(subjectIds::contains)
                .distinct()
                .count();
        return new SubjectStats(subjects.size(), withStages, stages.size(),
                subjectMapper.countAllQuestions());
    }

    /** A-S02 学科卡片列表（按 sort_order、id 升序）。 */
    public List<SubjectCard> list() {
        List<Subject> subjects = subjectMapper.selectList(Wrappers.<Subject>lambdaQuery()
                .orderByAsc(Subject::getSortOrder)
                .orderByAsc(Subject::getId));
        List<Stage> stages = stageMapper.selectList(Wrappers.<Stage>lambdaQuery()
                .orderByAsc(Stage::getSortOrder)
                .orderByAsc(Stage::getId));

        Map<Long, List<Stage>> stagesBySubject = stages.stream()
                .collect(Collectors.groupingBy(Stage::getSubjectId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, Long> questionBySubject = toCountMap(subjectMapper.countQuestionsBySubject());
        Map<Long, Long> questionByStage = toCountMap(subjectMapper.countQuestionsByStage());
        Map<Long, Long> courseBySubject = toCountMap(subjectMapper.countCoursesBySubject());
        Map<Long, Long> routeBySubject = toCountMap(subjectMapper.countRoutesBySubject());

        return subjects.stream().map(subject -> {
            List<StageItem> items = stagesBySubject.getOrDefault(subject.getId(), List.of()).stream()
                    .map(stage -> new StageItem(stage.getId(), stage.getName(), stage.getIcon(),
                            nullSafe(stage.getSortOrder()), nullSafe(stage.getStatus()),
                            questionByStage.getOrDefault(stage.getId(), 0L)))
                    .toList();
            return new SubjectCard(subject.getId(), subject.getName(), subject.getIcon(),
                    subject.getDescription(), subject.getThemeColor(), nullSafe(subject.getSortOrder()),
                    courseBySubject.getOrDefault(subject.getId(), 0L),
                    routeBySubject.getOrDefault(subject.getId(), 0L),
                    questionBySubject.getOrDefault(subject.getId(), 0L), items);
        }).toList();
    }

    /** A-S03 新增学科（名称唯一，30001）。 */
    @Transactional
    public Long create(SubjectRequest request) {
        String name = request.name().trim();
        ensureSubjectNameUnique(name, null);

        Subject subject = new Subject();
        apply(subject, request);
        try {
            subjectMapper.insert(subject);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.NAME_EXISTS, "学科名称已存在");
        }
        operationLogRecorder.record(OperationActions.CREATE, TARGET_SUBJECT, name);
        return subject.getId();
    }

    /** A-S04 编辑学科。 */
    @Transactional
    public void update(Long id, SubjectRequest request) {
        Subject subject = requireSubject(id);
        String name = request.name().trim();
        ensureSubjectNameUnique(name, id);

        apply(subject, request);
        try {
            subjectMapper.updateById(subject);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.NAME_EXISTS, "学科名称已存在");
        }
        operationLogRecorder.record(OperationActions.UPDATE, TARGET_SUBJECT, name);
    }

    /** A-S05 学科上移 / 下移：交换相邻顺序后重排 1..n，保证顺序稳定。 */
    @Transactional
    public void move(SortRequest request) {
        List<Subject> subjects = subjectMapper.selectList(Wrappers.<Subject>lambdaQuery()
                .orderByAsc(Subject::getSortOrder)
                .orderByAsc(Subject::getId));
        Subject subject = subjects.stream()
                .filter(item -> item.getId().equals(request.id()))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "学科不存在"));
        if (!swap(subjects, subjects.indexOf(subject), request.move())) {
            return;
        }
        for (int index = 0; index < subjects.size(); index++) {
            Subject item = subjects.get(index);
            subjectMapper.update(null, Wrappers.<Subject>lambdaUpdate()
                    .eq(Subject::getId, item.getId())
                    .set(Subject::getSortOrder, index + 1));
        }
        operationLogRecorder.record(OperationActions.UPDATE, TARGET_SUBJECT,
                subject.getName() + (MOVE_UP.equals(request.move()) ? " 上移" : " 下移"));
    }

    /**
     * A-S06 删除学科：存在阶段 / 题目 / 课程 / 路线关联时 30007；
     * 无关联时逻辑删除并释放名称唯一键。
     */
    @Transactional
    public void delete(Long id) {
        Subject subject = requireSubject(id);
        long stageCount = stageMapper.selectCount(Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getSubjectId, id));
        long questionCount = stageMapper.countQuestionsBySubject(id);
        long courseCount = subjectMapper.countCourses(id);
        long routeCount = subjectMapper.countRoutes(id);
        if (stageCount > 0 || questionCount > 0 || courseCount > 0 || routeCount > 0) {
            throw new BizException(ErrorCode.RELATED_CONTENT_EXISTS,
                    deleteBlockedMessage(stageCount, questionCount, courseCount, routeCount));
        }
        releaseSubjectName(subject);
        subjectMapper.deleteById(id);
        operationLogRecorder.record(OperationActions.DELETE, TARGET_SUBJECT, subject.getName());
    }

    /** A-S07 新增阶段（同一学科内名称唯一，30001）。 */
    @Transactional
    public Long createStage(Long subjectId, StageRequest request) {
        requireSubject(subjectId);
        String name = request.name().trim();
        ensureStageNameUnique(subjectId, name, null);

        Stage stage = new Stage();
        stage.setSubjectId(subjectId);
        apply(stage, request);
        stage.setStatus(1);
        try {
            stageMapper.insert(stage);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.NAME_EXISTS, "该学科下阶段名称已存在");
        }
        operationLogRecorder.record(OperationActions.CREATE, TARGET_STAGE, name);
        return stage.getId();
    }

    /** A-S08 编辑阶段。 */
    @Transactional
    public void updateStage(Long subjectId, Long id, StageRequest request) {
        Stage stage = requireStage(subjectId, id);
        String name = request.name().trim();
        ensureStageNameUnique(subjectId, name, id);

        apply(stage, request);
        try {
            // 显式 set：阶段图标允许清空为 null（updateById 默认会忽略 null 字段）
            stageMapper.update(null, Wrappers.<Stage>lambdaUpdate()
                    .eq(Stage::getId, id)
                    .set(Stage::getName, stage.getName())
                    .set(Stage::getIcon, stage.getIcon())
                    .set(Stage::getSortOrder, stage.getSortOrder()));
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.NAME_EXISTS, "该学科下阶段名称已存在");
        }
        operationLogRecorder.record(OperationActions.UPDATE, TARGET_STAGE, name);
    }

    /** A-S09 阶段上移 / 下移（仅影响所属学科内的顺序）。 */
    @Transactional
    public void moveStage(Long subjectId, SortRequest request) {
        requireSubject(subjectId);
        List<Stage> stages = stageMapper.selectList(Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getSubjectId, subjectId)
                .orderByAsc(Stage::getSortOrder)
                .orderByAsc(Stage::getId));
        Stage stage = stages.stream()
                .filter(item -> item.getId().equals(request.id()))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "阶段不存在"));
        if (!swap(stages, stages.indexOf(stage), request.move())) {
            return;
        }
        for (int index = 0; index < stages.size(); index++) {
            Stage item = stages.get(index);
            stageMapper.update(null, Wrappers.<Stage>lambdaUpdate()
                    .eq(Stage::getId, item.getId())
                    .set(Stage::getSortOrder, index + 1));
        }
        operationLogRecorder.record(OperationActions.UPDATE, TARGET_STAGE,
                stage.getName() + (MOVE_UP.equals(request.move()) ? " 上移" : " 下移"));
    }

    /** A-S10 启用 / 停用阶段。 */
    @Transactional
    public void updateStageStatus(Long subjectId, Long id, Integer status) {
        Stage stage = requireStage(subjectId, id);
        stage.setStatus(status);
        stageMapper.updateById(stage);
        operationLogRecorder.record(OperationActions.STATUS, TARGET_STAGE,
                stage.getName() + (status == 1 ? " 启用" : " 停用"));
    }

    /**
     * 题库导入等场景：按学科 + 阶段名查找阶段，不存在时自动创建（启用、追加到末尾）。
     *
     * <p>同一学科内阶段名唯一；并发导入撞唯一键时回查已创建的阶段。</p>
     */
    @Transactional
    public Long findOrCreateStage(Long subjectId, String name) {
        requireSubject(subjectId);
        String stageName = name.trim();
        Stage existing = stageMapper.selectOne(Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getSubjectId, subjectId)
                .eq(Stage::getName, stageName));
        if (existing != null) {
            return existing.getId();
        }
        int maxSortOrder = stageMapper.selectList(Wrappers.<Stage>lambdaQuery()
                        .eq(Stage::getSubjectId, subjectId)).stream()
                .mapToInt(stage -> nullSafe(stage.getSortOrder()))
                .max()
                .orElse(0);
        Stage stage = new Stage();
        stage.setSubjectId(subjectId);
        stage.setName(stageName);
        stage.setSortOrder(maxSortOrder + 1);
        stage.setStatus(1);
        try {
            stageMapper.insert(stage);
            return stage.getId();
        } catch (DuplicateKeyException ex) {
            Stage created = stageMapper.selectOne(Wrappers.<Stage>lambdaQuery()
                    .eq(Stage::getSubjectId, subjectId)
                    .eq(Stage::getName, stageName));
            if (created == null) {
                throw ex;
            }
            return created.getId();
        }
    }

    /** A-S11 删除阶段：下有题目 → 30007；否则逻辑删除并释放名称唯一键。 */
    @Transactional
    public void deleteStage(Long subjectId, Long id) {
        Stage stage = requireStage(subjectId, id);
        long questionCount = stageMapper.countQuestions(id);
        if (questionCount > 0) {
            throw new BizException(ErrorCode.RELATED_CONTENT_EXISTS,
                    "该阶段下有 " + questionCount + " 道题，请先处理题目后再删除阶段");
        }
        releaseStageName(stage);
        stageMapper.deleteById(id);
        operationLogRecorder.record(OperationActions.DELETE, TARGET_STAGE, stage.getName());
    }

    private Subject requireSubject(Long id) {
        Subject subject = subjectMapper.selectById(id);
        if (subject == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学科不存在");
        }
        return subject;
    }

    private Stage requireStage(Long subjectId, Long id) {
        Stage stage = stageMapper.selectById(id);
        if (stage == null || !stage.getSubjectId().equals(subjectId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "阶段不存在");
        }
        return stage;
    }

    private void ensureSubjectNameUnique(String name, Long excludeId) {
        var query = Wrappers.<Subject>lambdaQuery().eq(Subject::getName, name);
        if (excludeId != null) {
            query.ne(Subject::getId, excludeId);
        }
        if (subjectMapper.selectCount(query) > 0) {
            throw new BizException(ErrorCode.NAME_EXISTS, "学科名称已存在");
        }
    }

    private void ensureStageNameUnique(Long subjectId, String name, Long excludeId) {
        var query = Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getSubjectId, subjectId)
                .eq(Stage::getName, name);
        if (excludeId != null) {
            query.ne(Stage::getId, excludeId);
        }
        if (stageMapper.selectCount(query) > 0) {
            throw new BizException(ErrorCode.NAME_EXISTS, "该学科下阶段名称已存在");
        }
    }

    private void apply(Subject subject, SubjectRequest request) {
        subject.setName(request.name().trim());
        subject.setIcon(request.icon().trim());
        subject.setDescription(request.description().trim());
        subject.setThemeColor(request.themeColor());
        subject.setSortOrder(request.sortOrder());
    }

    private void apply(Stage stage, StageRequest request) {
        stage.setName(request.name().trim());
        stage.setIcon(StringUtils.hasText(request.icon()) ? request.icon().trim() : null);
        stage.setSortOrder(request.sortOrder());
    }

    /**
     * 交换目标记录与相邻记录；已在边界时返回 {@code false}。
     */
    private boolean swap(List<?> items, int index, String move) {
        int target = MOVE_UP.equals(move) ? index - 1 : index + 1;
        if (target < 0 || target >= items.size()) {
            return false;
        }
        java.util.Collections.swap(items, index, target);
        return true;
    }

    private String deleteBlockedMessage(long stageCount, long questionCount,
                                        long courseCount, long routeCount) {
        List<String> relations = new ArrayList<>();
        if (stageCount > 0 && questionCount > 0) {
            relations.add("下有 " + stageCount + " 个阶段与 " + questionCount + " 道题");
        } else if (stageCount > 0) {
            relations.add("下有 " + stageCount + " 个阶段");
        } else if (questionCount > 0) {
            relations.add("下有 " + questionCount + " 道题");
        }
        if (courseCount > 0) {
            relations.add("关联 " + courseCount + " 门课程");
        }
        if (routeCount > 0) {
            relations.add("关联 " + routeCount + " 条学习路线");
        }
        return "该学科" + String.join("、", relations) + "，请先处理后再删除";
    }

    /**
     * 逻辑删除前释放名称唯一键：追加 {@code #del{id}} 并按 50 字符上限截断。
     */
    private void releaseSubjectName(Subject subject) {
        String released = releasedName(subject.getName(), subject.getId());
        subjectMapper.update(null, Wrappers.<Subject>lambdaUpdate()
                .eq(Subject::getId, subject.getId())
                .set(Subject::getName, released));
    }

    private void releaseStageName(Stage stage) {
        String released = releasedName(stage.getName(), stage.getId());
        stageMapper.update(null, Wrappers.<Stage>lambdaUpdate()
                .eq(Stage::getId, stage.getId())
                .set(Stage::getName, released));
    }

    private String releasedName(String name, Long id) {
        String suffix = "#del" + id;
        int keep = Math.max(0, 50 - suffix.length());
        return (name.length() > keep ? name.substring(0, keep) : name) + suffix;
    }

    private Map<Long, Long> toCountMap(List<IdCountRow> rows) {
        return rows.stream().collect(Collectors.toMap(IdCountRow::getId, IdCountRow::getTotal, (a, b) -> a));
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}

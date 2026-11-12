package com.yunshu.zhixue.modules.route.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.course.dto.SubjectRef;
import com.yunshu.zhixue.modules.course.entity.Course;
import com.yunshu.zhixue.modules.course.entity.CourseSubject;
import com.yunshu.zhixue.modules.course.mapper.CourseMapper;
import com.yunshu.zhixue.modules.course.mapper.CourseSubjectMapper;
import com.yunshu.zhixue.modules.route.dto.RouteCard;
import com.yunshu.zhixue.modules.route.dto.RouteCourseItem;
import com.yunshu.zhixue.modules.route.dto.RouteCourseOption;
import com.yunshu.zhixue.modules.route.dto.RouteCoursePreview;
import com.yunshu.zhixue.modules.route.dto.RouteDetail;
import com.yunshu.zhixue.modules.route.dto.RouteRequest;
import com.yunshu.zhixue.modules.route.entity.LearningRoute;
import com.yunshu.zhixue.modules.route.entity.RouteCourse;
import com.yunshu.zhixue.modules.route.mapper.LearningRouteMapper;
import com.yunshu.zhixue.modules.route.mapper.RouteCourseMapper;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学习路线管理（A-R01 ~ A-R09）。
 *
 * <p>一条路线归属单个学科，课程顺序即学员学习顺序；课程可跨学科补充。
 * 移除课程采用逻辑删除，重新添加时恢复原 route_course 行以复用唯一键。</p>
 */
@Service
public class LearningRouteService {

    private static final String TARGET = "学习路线";
    /** 「从课程库添加」单次返回的候选课程上限。 */
    private static final int COURSE_OPTION_LIMIT = 300;
    private static final int PREVIEW_COURSE_LIMIT = 4;

    private final LearningRouteMapper routeMapper;
    private final RouteCourseMapper routeCourseMapper;
    private final CourseMapper courseMapper;
    private final CourseSubjectMapper courseSubjectMapper;
    private final SubjectMapper subjectMapper;
    private final OperationLogRecorder operationLogRecorder;

    public LearningRouteService(LearningRouteMapper routeMapper,
                                RouteCourseMapper routeCourseMapper,
                                CourseMapper courseMapper,
                                CourseSubjectMapper courseSubjectMapper,
                                SubjectMapper subjectMapper,
                                OperationLogRecorder operationLogRecorder) {
        this.routeMapper = routeMapper;
        this.routeCourseMapper = routeCourseMapper;
        this.courseMapper = courseMapper;
        this.courseSubjectMapper = courseSubjectMapper;
        this.subjectMapper = subjectMapper;
        this.operationLogRecorder = operationLogRecorder;
    }

    /** A-R01 路线卡片列表（含课程数、总时长与前 4 门课程预览）。 */
    public List<RouteCard> list() {
        List<LearningRoute> routes = routeMapper.selectList(Wrappers.<LearningRoute>lambdaQuery()
                .orderByDesc(LearningRoute::getId));
        if (routes.isEmpty()) {
            return List.of();
        }

        Map<Long, Subject> subjects = loadSubjects(routes.stream()
                .map(LearningRoute::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        List<RouteCourse> relations = routeCourseMapper.selectList(Wrappers.<RouteCourse>lambdaQuery()
                .in(RouteCourse::getRouteId, routes.stream().map(LearningRoute::getId).toList())
                .orderByAsc(RouteCourse::getSortOrder)
                .orderByAsc(RouteCourse::getId));
        Map<Long, List<RouteCourse>> relationsByRoute = relations.stream()
                .collect(Collectors.groupingBy(RouteCourse::getRouteId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, Course> courses = loadCourses(relations.stream()
                .map(RouteCourse::getCourseId)
                .collect(Collectors.toSet()));

        return routes.stream().map(route -> {
            List<Course> routeCourses = relationsByRoute.getOrDefault(route.getId(), List.of()).stream()
                    .map(relation -> courses.get(relation.getCourseId()))
                    .filter(Objects::nonNull)
                    .toList();
            long totalMinutes = routeCourses.stream()
                    .mapToLong(course -> nullSafe(course.getDurationMinutes()))
                    .sum();
            List<RouteCoursePreview> previews = routeCourses.stream()
                    .limit(PREVIEW_COURSE_LIMIT)
                    .map(course -> new RouteCoursePreview(course.getId(), course.getName()))
                    .toList();
            Long subjectId = route.getSubjectId();
            Subject subject = subjectId == null ? null : subjects.get(subjectId);
            return new RouteCard(route.getId(), route.getName(), route.getIcon(),
                    nullSafe(route.getSubjectId()), subject == null ? null : subject.getName(),
                    nullSafe(route.getStatus()), route.getTargetEducation(), route.getDescription(),
                    routeCourses.size(), totalMinutes, previews);
        }).toList();
    }

    /** 路线表单的学科下拉。 */
    public List<SubjectRef> subjectOptions() {
        return subjectMapper.selectList(Wrappers.<Subject>lambdaQuery()
                        .orderByAsc(Subject::getSortOrder)
                        .orderByAsc(Subject::getId)).stream()
                .map(subject -> new SubjectRef(subject.getId(), subject.getName()))
                .toList();
    }

    /** A-R02 新建路线（允许重名，与 03 一致）。 */
    @Transactional
    public Long create(RouteRequest request) {
        requireSubject(request.subjectId());
        LearningRoute route = new LearningRoute();
        route.setName(request.name().trim());
        route.setSubjectId(request.subjectId());
        route.setStatus(request.status() == null ? 1 : request.status());
        route.setIcon(trimToNull(request.icon()));
        route.setTargetEducation(trimToNull(request.targetEducation()));
        route.setDescription(trimToNull(request.description()));
        routeMapper.insert(route);
        operationLogRecorder.record(OperationActions.CREATE, TARGET, route.getName());
        return route.getId();
    }

    /** A-R03 编辑路线基本信息。 */
    @Transactional
    public void update(Long id, RouteRequest request) {
        LearningRoute route = requireRoute(id);
        requireSubject(request.subjectId());
        int status = request.status() == null ? nullSafe(route.getStatus()) : request.status();
        routeMapper.update(null, Wrappers.<LearningRoute>lambdaUpdate()
                .eq(LearningRoute::getId, id)
                .set(LearningRoute::getName, request.name().trim())
                .set(LearningRoute::getSubjectId, request.subjectId())
                .set(LearningRoute::getStatus, status)
                .set(LearningRoute::getIcon, trimToNull(request.icon()))
                .set(LearningRoute::getTargetEducation, trimToNull(request.targetEducation()))
                .set(LearningRoute::getDescription, trimToNull(request.description())));
        operationLogRecorder.record(OperationActions.UPDATE, TARGET, request.name().trim());
    }

    /** A-R04 启用 / 停用。 */
    @Transactional
    public void updateStatus(Long id, Integer status) {
        LearningRoute route = requireRoute(id);
        routeMapper.update(null, Wrappers.<LearningRoute>lambdaUpdate()
                .eq(LearningRoute::getId, id)
                .set(LearningRoute::getStatus, status));
        operationLogRecorder.record(OperationActions.STATUS, TARGET,
                route.getName() + (status == 1 ? " 启用" : " 停用"));
    }

    /** A-R05 删除路线：级联逻辑删除 route_course（学员已完成记录保留）。 */
    @Transactional
    public void delete(Long id) {
        LearningRoute route = requireRoute(id);
        routeCourseMapper.delete(Wrappers.<RouteCourse>lambdaQuery()
                .eq(RouteCourse::getRouteId, id));
        routeMapper.deleteById(id);
        operationLogRecorder.record(OperationActions.DELETE, TARGET, route.getName());
    }

    /** A-R06 详情 / 预览：按学习顺序返回课程，并标注本学科 / 跨学科。 */
    public RouteDetail detail(Long id) {
        LearningRoute route = requireRoute(id);
        Long routeSubjectId = route.getSubjectId();
        long routeSubjectValue = nullSafe(routeSubjectId);
        Subject subject = routeSubjectId == null ? null : subjectMapper.selectById(routeSubjectId);

        List<RouteCourse> relations = routeCourseMapper.selectList(Wrappers.<RouteCourse>lambdaQuery()
                .eq(RouteCourse::getRouteId, id)
                .orderByAsc(RouteCourse::getSortOrder)
                .orderByAsc(RouteCourse::getId));
        Map<Long, Course> courses = loadCourses(relations.stream()
                .map(RouteCourse::getCourseId)
                .collect(Collectors.toSet()));
        Map<Long, List<SubjectRef>> courseSubjects = loadCourseSubjects(courses.values());

        List<RouteCourseItem> items = relations.stream()
                .map(relation -> {
                    Course course = courses.get(relation.getCourseId());
                    if (course == null) {
                        return null;
                    }
                    List<SubjectRef> refs = courseSubjects.getOrDefault(course.getId(), List.of());
                    boolean sameSubject = refs.stream()
                            .anyMatch(ref -> ref.id() == routeSubjectValue);
                    return new RouteCourseItem(relation.getId(), course.getId(), course.getName(),
                            course.getBvid(), nullSafe(course.getDurationMinutes()),
                            nullSafe(course.getStatus()), refs, sameSubject,
                            nullSafe(relation.getSortOrder()));
                })
                .filter(Objects::nonNull)
                .toList();
        long totalMinutes = items.stream().mapToLong(RouteCourseItem::durationMinutes).sum();

        return new RouteDetail(route.getId(), route.getName(), route.getIcon(),
                nullSafe(route.getSubjectId()), subject == null ? null : subject.getName(),
                nullSafe(route.getStatus()), route.getTargetEducation(), route.getDescription(),
                items.size(), totalMinutes, items);
    }

    /** A-R07 课程库候选：本学科课程排前，含已下架课程与是否已添加标记。 */
    public List<RouteCourseOption> courseOptions(Long routeId, String keyword) {
        LearningRoute route = requireRoute(routeId);
        var query = Wrappers.<Course>lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(w -> w.like(Course::getName, value).or().like(Course::getBvid, value));
        }
        query.orderByDesc(Course::getId).last("LIMIT " + COURSE_OPTION_LIMIT);
        List<Course> courses = courseMapper.selectList(query);
        if (courses.isEmpty()) {
            return List.of();
        }

        Set<Long> addedCourseIds = routeCourseMapper.selectList(Wrappers.<RouteCourse>lambdaQuery()
                        .eq(RouteCourse::getRouteId, routeId)).stream()
                .map(RouteCourse::getCourseId)
                .collect(Collectors.toSet());
        Map<Long, List<SubjectRef>> courseSubjects = loadCourseSubjects(courses);
        long routeSubjectValue = nullSafe(route.getSubjectId());

        return courses.stream().map(course -> {
            List<SubjectRef> refs = courseSubjects.getOrDefault(course.getId(), List.of());
            boolean sameSubject = refs.stream()
                    .anyMatch(ref -> ref.id() == routeSubjectValue);
            return new RouteCourseOption(course.getId(), course.getName(), course.getBvid(),
                    nullSafe(course.getDurationMinutes()), nullSafe(course.getStatus()),
                    refs, sameSubject, addedCourseIds.contains(course.getId()));
        }).toList();
    }

    /** A-R07 批量添加课程，追加到路线末尾。 */
    @Transactional
    public int addCourses(Long routeId, List<Long> courseIds) {
        requireRoute(routeId);
        LinkedHashSet<Long> ids = courseIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return 0;
        }
        Set<Long> existingCourses = courseMapper.selectBatchIds(ids).stream()
                .map(Course::getId)
                .collect(Collectors.toSet());
        if (existingCourses.size() != ids.size()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "部分课程不存在或已删除");
        }

        List<RouteCourse> relations = routeCourseMapper.selectList(Wrappers.<RouteCourse>lambdaQuery()
                .eq(RouteCourse::getRouteId, routeId));
        Set<Long> addedCourseIds = relations.stream()
                .map(RouteCourse::getCourseId)
                .collect(Collectors.toSet());
        int nextSortOrder = relations.stream()
                .mapToInt(relation -> nullSafe(relation.getSortOrder()))
                .max()
                .orElse(0);

        int added = 0;
        for (Long courseId : ids) {
            if (addedCourseIds.contains(courseId)) {
                continue;
            }
            nextSortOrder++;
            RouteCourse deleted = routeCourseMapper.findIncludingDeleted(routeId, courseId);
            if (deleted != null) {
                routeCourseMapper.restore(deleted.getId(), nextSortOrder);
            } else {
                RouteCourse relation = new RouteCourse();
                relation.setRouteId(routeId);
                relation.setCourseId(courseId);
                relation.setSortOrder(nextSortOrder);
                routeCourseMapper.insert(relation);
            }
            added++;
        }
        if (added > 0) {
            operationLogRecorder.record(OperationActions.UPDATE, TARGET,
                    "添加 " + added + " 门课程");
        }
        return added;
    }

    /** A-R08 从路线移除课程。 */
    @Transactional
    public void removeCourse(Long routeId, Long courseId) {
        requireRoute(routeId);
        RouteCourse relation = routeCourseMapper.selectOne(Wrappers.<RouteCourse>lambdaQuery()
                .eq(RouteCourse::getRouteId, routeId)
                .eq(RouteCourse::getCourseId, courseId));
        if (relation == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不在该路线中");
        }
        Course course = courseMapper.selectById(courseId);
        routeCourseMapper.deleteById(relation.getId());
        operationLogRecorder.record(OperationActions.DELETE, TARGET,
                "移除课程：" + (course == null ? "#" + courseId : course.getName()));
    }

    /** A-R09 课程排序：提交完整顺序数组，按索引重排 1..n。 */
    @Transactional
    public void sortCourses(Long routeId, List<Long> orderedIds) {
        requireRoute(routeId);
        List<RouteCourse> relations = routeCourseMapper.selectList(Wrappers.<RouteCourse>lambdaQuery()
                .eq(RouteCourse::getRouteId, routeId));
        Set<Long> currentIds = relations.stream().map(RouteCourse::getId).collect(Collectors.toSet());
        Set<Long> requestedIds = new HashSet<>(orderedIds);
        if (currentIds.size() != requestedIds.size() || !currentIds.equals(requestedIds)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "课程顺序已变化，请刷新后重试");
        }
        for (int index = 0; index < orderedIds.size(); index++) {
            routeCourseMapper.update(null, Wrappers.<RouteCourse>lambdaUpdate()
                    .eq(RouteCourse::getId, orderedIds.get(index))
                    .set(RouteCourse::getSortOrder, index + 1));
        }
        operationLogRecorder.record(OperationActions.UPDATE, TARGET, "调整课程顺序");
    }

    private LearningRoute requireRoute(Long id) {
        LearningRoute route = routeMapper.selectById(id);
        if (route == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学习路线不存在");
        }
        return route;
    }

    private void requireSubject(Long subjectId) {
        if (subjectMapper.selectById(subjectId) == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "关联学科不存在");
        }
    }

    private Map<Long, Subject> loadSubjects(Collection<Long> subjectIds) {
        if (subjectIds.isEmpty()) {
            return Map.of();
        }
        return subjectMapper.selectBatchIds(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, Course> loadCourses(Collection<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        return courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity(), (a, b) -> a));
    }

    /** 批量装配课程关联的学科（主学科在前，额外学科按关联 id 排序）。 */
    private Map<Long, List<SubjectRef>> loadCourseSubjects(Collection<Course> courses) {
        if (courses.isEmpty()) {
            return Map.of();
        }
        Set<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toSet());
        Map<Long, List<CourseSubject>> relations = courseSubjectMapper.selectList(
                        Wrappers.<CourseSubject>lambdaQuery()
                                .in(CourseSubject::getCourseId, courseIds)
                                .orderByAsc(CourseSubject::getId)).stream()
                .collect(Collectors.groupingBy(CourseSubject::getCourseId,
                        LinkedHashMap::new, Collectors.toList()));

        Set<Long> subjectIds = new LinkedHashSet<>();
        for (Course course : courses) {
            if (course.getSubjectId() != null) {
                subjectIds.add(course.getSubjectId());
            }
        }
        relations.values().forEach(list -> list.forEach(relation -> subjectIds.add(relation.getSubjectId())));
        Map<Long, Subject> subjects = loadSubjects(subjectIds);

        Map<Long, List<SubjectRef>> result = new LinkedHashMap<>();
        for (Course course : courses) {
            List<SubjectRef> refs = new ArrayList<>();
            Subject primary = course.getSubjectId() == null ? null : subjects.get(course.getSubjectId());
            if (primary != null) {
                refs.add(new SubjectRef(primary.getId(), primary.getName()));
            }
            for (CourseSubject relation : relations.getOrDefault(course.getId(), List.of())) {
                Subject subject = subjects.get(relation.getSubjectId());
                if (subject != null && !subject.getId().equals(course.getSubjectId())) {
                    refs.add(new SubjectRef(subject.getId(), subject.getName()));
                }
            }
            result.put(course.getId(), refs);
        }
        return result;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}

package com.yunshu.zhixue.modules.route.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.course.entity.Course;
import com.yunshu.zhixue.modules.course.mapper.CourseMapper;
import com.yunshu.zhixue.modules.route.dto.PortalRouteCard;
import com.yunshu.zhixue.modules.route.dto.PortalRouteCourseItem;
import com.yunshu.zhixue.modules.route.dto.PortalRouteDetail;
import com.yunshu.zhixue.modules.route.dto.PortalRouteSubjectItem;
import com.yunshu.zhixue.modules.route.dto.RouteCourseCompleteRequest;
import com.yunshu.zhixue.modules.route.entity.LearningRoute;
import com.yunshu.zhixue.modules.route.entity.RouteCourse;
import com.yunshu.zhixue.modules.route.entity.RouteCourseCompletion;
import com.yunshu.zhixue.modules.route.mapper.LearningRouteMapper;
import com.yunshu.zhixue.modules.route.mapper.RouteCourseCompletionMapper;
import com.yunshu.zhixue.modules.route.mapper.RouteCourseMapper;
import com.yunshu.zhixue.modules.student.entity.Student;
import com.yunshu.zhixue.modules.student.mapper.StudentMapper;
import com.yunshu.zhixue.modules.subject.dto.IdCountRow;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.security.SecurityUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学员端学习路线（P-R01 ~ P-R03）。
 *
 * <p>学员只看到启用路线与上架课程；课程完成由学员手动勾选产生，
 * 取消勾选采用逻辑删除，重新勾选时恢复原记录以复用唯一键。</p>
 */
@Service
public class PortalLearningRouteService {

    private final LearningRouteMapper routeMapper;
    private final RouteCourseMapper routeCourseMapper;
    private final CourseMapper courseMapper;
    private final SubjectMapper subjectMapper;
    private final RouteCourseCompletionMapper completionMapper;
    private final StudentMapper studentMapper;

    public PortalLearningRouteService(LearningRouteMapper routeMapper,
                                      RouteCourseMapper routeCourseMapper,
                                      CourseMapper courseMapper,
                                      SubjectMapper subjectMapper,
                                      RouteCourseCompletionMapper completionMapper,
                                      StudentMapper studentMapper) {
        this.routeMapper = routeMapper;
        this.routeCourseMapper = routeCourseMapper;
        this.courseMapper = courseMapper;
        this.subjectMapper = subjectMapper;
        this.completionMapper = completionMapper;
        this.studentMapper = studentMapper;
    }

    /** 学科选择卡：全部学科 + 上架课程数 + 是否有启用路线。 */
    public List<PortalRouteSubjectItem> subjects() {
        currentStudentId();
        List<Subject> subjects = subjectMapper.selectList(Wrappers.<Subject>lambdaQuery()
                .orderByAsc(Subject::getSortOrder)
                .orderByAsc(Subject::getId));
        if (subjects.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> courseCounts = toCountMap(subjectMapper.countPublishedCoursesBySubject());
        Map<Long, LearningRoute> routesBySubject = enabledRoutes().stream()
                .filter(route -> route.getSubjectId() != null)
                .collect(Collectors.toMap(LearningRoute::getSubjectId, Function.identity(),
                        (first, ignored) -> first));

        return subjects.stream()
                .map(subject -> {
                    LearningRoute route = routesBySubject.get(subject.getId());
                    return new PortalRouteSubjectItem(
                            subject.getId(),
                            subject.getName(),
                            subject.getIcon(),
                            courseCounts.getOrDefault(subject.getId(), 0L),
                            route == null ? null : route.getId(),
                            route == null ? null : route.getName());
                })
                .toList();
    }

    /** P-R01 学员可见路线列表，含完成数与进度。 */
    public List<PortalRouteCard> routes() {
        long studentId = currentStudentId();
        List<LearningRoute> routes = enabledRoutes();
        if (routes.isEmpty()) {
            return List.of();
        }

        Map<Long, List<RouteCourse>> relationsByRoute = routeCourseMapper.selectList(
                        Wrappers.<RouteCourse>lambdaQuery()
                                .in(RouteCourse::getRouteId, routes.stream().map(LearningRoute::getId).toList())
                                .orderByAsc(RouteCourse::getSortOrder)
                                .orderByAsc(RouteCourse::getId)).stream()
                .collect(Collectors.groupingBy(RouteCourse::getRouteId));
        Map<Long, Course> courses = loadPublishedCourses(relationsByRoute.values().stream()
                .flatMap(List::stream)
                .map(RouteCourse::getCourseId)
                .collect(Collectors.toSet()));
        Set<Long> completedIds = Set.copyOf(
                completionMapper.selectCompletedRouteCourseIds(studentId));
        Map<Long, Subject> subjects = loadSubjects(routes.stream()
                .map(LearningRoute::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        return routes.stream().map(route -> {
            List<RouteCourse> activeRelations = relationsByRoute.getOrDefault(route.getId(), List.of()).stream()
                    .filter(relation -> courses.containsKey(relation.getCourseId()))
                    .toList();
            long courseCount = activeRelations.size();
            long completedCount = activeRelations.stream()
                    .filter(relation -> completedIds.contains(relation.getId()))
                    .count();
            Subject subject = route.getSubjectId() == null ? null : subjects.get(route.getSubjectId());
            return new PortalRouteCard(
                    route.getId(),
                    route.getName(),
                    nullSafe(route.getSubjectId()),
                    subject == null ? null : subject.getName(),
                    route.getTargetEducation(),
                    route.getDescription(),
                    courseCount,
                    completedCount,
                    progress(completedCount, courseCount));
        }).toList();
    }

    /** P-R02 路线详情：按顺序返回上架课程与完成状态。 */
    public PortalRouteDetail detail(long routeId) {
        long studentId = currentStudentId();
        LearningRoute route = routeMapper.selectById(routeId);
        if (route == null || !Objects.equals(route.getStatus(), 1)) {
            throw new BizException(ErrorCode.NOT_FOUND, "学习路线不存在或已停用");
        }
        Subject subject = route.getSubjectId() == null
                ? null
                : subjectMapper.selectById(route.getSubjectId());

        List<RouteCourse> relations = routeCourseMapper.selectList(Wrappers.<RouteCourse>lambdaQuery()
                .eq(RouteCourse::getRouteId, routeId)
                .orderByAsc(RouteCourse::getSortOrder)
                .orderByAsc(RouteCourse::getId));
        Map<Long, Course> courses = loadPublishedCourses(relations.stream()
                .map(RouteCourse::getCourseId)
                .collect(Collectors.toSet()));
        Set<Long> completedIds = Set.copyOf(
                completionMapper.selectCompletedRouteCourseIds(studentId));

        List<PortalRouteCourseItem> items = relations.stream()
                .map(relation -> {
                    Course course = courses.get(relation.getCourseId());
                    if (course == null) {
                        return null;
                    }
                    return new PortalRouteCourseItem(
                            relation.getId(),
                            course.getId(),
                            course.getName(),
                            course.getBvid(),
                            nullSafe(course.getDurationMinutes()),
                            nullSafe(course.getSource()),
                            course.getCoverUrl(),
                            completedIds.contains(relation.getId()),
                            nullSafe(relation.getSortOrder()));
                })
                .filter(Objects::nonNull)
                .toList();

        return new PortalRouteDetail(
                route.getId(),
                route.getName(),
                nullSafe(route.getSubjectId()),
                subject == null ? null : subject.getName(),
                route.getTargetEducation(),
                route.getDescription(),
                items);
    }

    /** P-R03 标记课程完成，重复标记幂等。 */
    @Transactional
    public void markComplete(long routeCourseId, RouteCourseCompleteRequest request) {
        long studentId = currentStudentId();
        RouteCourse relation = routeCourseMapper.selectById(routeCourseId);
        if (relation == null || !Objects.equals(relation.getCourseId(), request.courseId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不在该路线中");
        }
        LearningRoute route = routeMapper.selectById(relation.getRouteId());
        if (route == null || !Objects.equals(route.getStatus(), 1)) {
            throw new BizException(ErrorCode.NOT_FOUND, "学习路线不存在或已停用");
        }
        Course course = courseMapper.selectById(relation.getCourseId());
        if (course == null || !Objects.equals(course.getStatus(), 1)) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不存在或已下架");
        }

        RouteCourseCompletion existing = completionMapper.selectOne(
                Wrappers.<RouteCourseCompletion>lambdaQuery()
                        .eq(RouteCourseCompletion::getStudentId, studentId)
                        .eq(RouteCourseCompletion::getRouteCourseId, routeCourseId));
        if (existing != null) {
            return;
        }

        LocalDateTime completedAt = LocalDateTime.now(ZoneOffset.UTC);
        RouteCourseCompletion deleted = completionMapper.findIncludingDeleted(studentId, routeCourseId);
        if (deleted != null) {
            completionMapper.restore(deleted.getId(), completedAt);
            return;
        }

        RouteCourseCompletion completion = new RouteCourseCompletion();
        completion.setStudentId(studentId);
        completion.setRouteCourseId(routeCourseId);
        completion.setCompletedAt(completedAt);
        try {
            completionMapper.insert(completion);
        } catch (DuplicateKeyException ex) {
            // 并发重复提交视为已标记成功
        }
    }

    /** 取消课程完成，幂等。 */
    @Transactional
    public void unmarkComplete(long routeCourseId) {
        long studentId = currentStudentId();
        completionMapper.delete(Wrappers.<RouteCourseCompletion>lambdaQuery()
                .eq(RouteCourseCompletion::getStudentId, studentId)
                .eq(RouteCourseCompletion::getRouteCourseId, routeCourseId));
    }

    private List<LearningRoute> enabledRoutes() {
        return routeMapper.selectList(Wrappers.<LearningRoute>lambdaQuery()
                .eq(LearningRoute::getStatus, 1)
                .orderByAsc(LearningRoute::getId));
    }

    private Map<Long, Course> loadPublishedCourses(Collection<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }
        return courseMapper.selectBatchIds(courseIds).stream()
                .filter(course -> Objects.equals(course.getStatus(), 1))
                .collect(Collectors.toMap(Course::getId, Function.identity(), (first, ignored) -> first));
    }

    private Map<Long, Subject> loadSubjects(Collection<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return Map.of();
        }
        return subjectMapper.selectBatchIds(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity(), (first, ignored) -> first));
    }

    private Map<Long, Long> toCountMap(List<IdCountRow> rows) {
        return rows.stream().collect(Collectors.toMap(IdCountRow::getId,
                row -> nullSafe(row.getTotal()), (first, ignored) -> first));
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

    private static double progress(long completed, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round(completed * 1000.0 / total) / 10.0;
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}

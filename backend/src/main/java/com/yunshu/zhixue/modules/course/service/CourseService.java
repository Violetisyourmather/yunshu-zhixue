package com.yunshu.zhixue.modules.course.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.course.dto.CourseItem;
import com.yunshu.zhixue.modules.course.dto.CourseRequest;
import com.yunshu.zhixue.modules.course.dto.CourseStats;
import com.yunshu.zhixue.modules.course.dto.SubjectRef;
import com.yunshu.zhixue.modules.course.entity.Course;
import com.yunshu.zhixue.modules.course.entity.CourseSubject;
import com.yunshu.zhixue.modules.course.mapper.CourseMapper;
import com.yunshu.zhixue.modules.course.mapper.CourseSubjectMapper;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogRecorder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * B 站课程库（A-C01 ~ A-C05、A-C08）。
 *
 * <p>课程-学科：主学科存 {@code course.subject_id}（NULL = 其他学科），
 * 额外学科存 {@code course_subject}；被学习路线引用的课程不可删除。</p>
 */
@Service
public class CourseService {

    private static final String TARGET = "课程";
    private static final int PAGE_SIZE_MAX = 100;
    private static final int SOURCE_BILIBILI = 1;
    private static final Pattern BVID_PATTERN = Pattern.compile("(?i)BV([0-9A-Za-z]{10})");

    private final CourseMapper courseMapper;
    private final CourseSubjectMapper courseSubjectMapper;
    private final SubjectMapper subjectMapper;
    private final OperationLogRecorder operationLogRecorder;

    public CourseService(CourseMapper courseMapper,
                         CourseSubjectMapper courseSubjectMapper,
                         SubjectMapper subjectMapper,
                         OperationLogRecorder operationLogRecorder) {
        this.courseMapper = courseMapper;
        this.courseSubjectMapper = courseSubjectMapper;
        this.subjectMapper = subjectMapper;
        this.operationLogRecorder = operationLogRecorder;
    }

    /** A-C01 统计卡。 */
    public CourseStats stats() {
        long courseCount = courseMapper.selectCount(null);
        long totalMinutes = courseMapper.sumDurationMinutes();
        double totalHours = Math.round(totalMinutes * 10.0 / 60.0) / 10.0;
        return new CourseStats(courseCount, totalMinutes, totalHours,
                courseMapper.countDistinctSubjects());
    }

    /** 课程表单与筛选的学科下拉（课程权限即可读取，避免依赖 subject:view）。 */
    public List<SubjectRef> subjectOptions() {
        return subjectMapper.selectList(Wrappers.<Subject>lambdaQuery()
                        .orderByAsc(Subject::getSortOrder)
                        .orderByAsc(Subject::getId)).stream()
                .map(subject -> new SubjectRef(subject.getId(), subject.getName()))
                .toList();
    }

    /** A-C02 分页列表；{@code subjectId=0} 表示主学科为空的「其他学科」。 */
    public PageResult<CourseItem> list(String keyword, Long subjectId, Integer source,
                                       Integer status, long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), PAGE_SIZE_MAX);

        var query = Wrappers.<Course>lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(w -> w.like(Course::getName, value).or().like(Course::getBvid, value));
        }
        if (subjectId != null) {
            if (subjectId == 0) {
                query.isNull(Course::getSubjectId);
            } else {
                long targetSubjectId = subjectId;
                query.and(w -> w.eq(Course::getSubjectId, targetSubjectId)
                        .or()
                        .inSql(Course::getId, "SELECT course_id FROM course_subject WHERE subject_id = "
                                + targetSubjectId + " AND delete_flag = 0"));
            }
        }
        if (source != null) {
            query.eq(Course::getSource, source);
        }
        if (status != null) {
            query.eq(Course::getStatus, status);
        }
        query.orderByDesc(Course::getId);

        Page<Course> result = courseMapper.selectPage(new Page<>(safePage, safePageSize), query);
        return PageResult.of(toItems(result.getRecords()), result.getTotal(), safePage, safePageSize);
    }

    /** A-C03 新增课程。 */
    @Transactional
    public Long create(CourseRequest request) {
        Course course = new Course();
        apply(course, request);
        ensureBvidUnique(course.getBvid(), null);
        List<Long> extraSubjects = normalizeExtraSubjects(course.getSubjectId(), request.subjectIds());
        try {
            courseMapper.insert(course);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.NAME_EXISTS, "BV 号已存在");
        }
        replaceSubjects(course.getId(), extraSubjects);
        operationLogRecorder.record(OperationActions.CREATE, TARGET, course.getName());
        return course.getId();
    }

    /** A-C04 编辑课程。 */
    @Transactional
    public void update(Long id, CourseRequest request) {
        Course course = requireCourse(id);
        apply(course, request);
        ensureBvidUnique(course.getBvid(), id);
        List<Long> extraSubjects = normalizeExtraSubjects(course.getSubjectId(), request.subjectIds());
        try {
            // 显式 set：允许把封面 / 简介 / 链接 / BV 号 / 主学科清空为 null
            courseMapper.update(null, Wrappers.<Course>lambdaUpdate()
                    .eq(Course::getId, id)
                    .set(Course::getName, course.getName())
                    .set(Course::getCoverUrl, course.getCoverUrl())
                    .set(Course::getIntro, course.getIntro())
                    .set(Course::getDurationMinutes, course.getDurationMinutes())
                    .set(Course::getSource, course.getSource())
                    .set(Course::getLink, course.getLink())
                    .set(Course::getBvid, course.getBvid())
                    .set(Course::getStatus, course.getStatus())
                    .set(Course::getSubjectId, course.getSubjectId()));
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.NAME_EXISTS, "BV 号已存在");
        }
        replaceSubjects(id, extraSubjects);
        operationLogRecorder.record(OperationActions.UPDATE, TARGET, course.getName());
    }

    /** A-C05 删除课程：被学习路线引用 → 30007；否则逻辑删除并释放 BV 唯一键。 */
    @Transactional
    public void delete(Long id) {
        Course course = requireCourse(id);
        long routeReferences = courseMapper.countRouteReferences(id);
        if (routeReferences > 0) {
            throw new BizException(ErrorCode.RELATED_CONTENT_EXISTS,
                    "该课程被 " + routeReferences + " 条学习路线引用，请先移除后再删除");
        }
        courseMapper.update(null, Wrappers.<Course>lambdaUpdate()
                .eq(Course::getId, id)
                .set(Course::getBvid, null));
        courseSubjectMapper.deleteByCourseId(id);
        courseMapper.deleteById(id);
        operationLogRecorder.record(OperationActions.DELETE, TARGET, course.getName());
    }

    /** A-C08 上架 / 下架。 */
    @Transactional
    public void updateStatus(Long id, Integer status) {
        Course course = requireCourse(id);
        course.setStatus(status);
        courseMapper.updateById(course);
        operationLogRecorder.record(OperationActions.STATUS, TARGET,
                course.getName() + (status == 1 ? " 上架" : " 下架"));
    }

    private Course requireCourse(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不存在");
        }
        return course;
    }

    private void apply(Course course, CourseRequest request) {
        String link = normalizeBilibiliUrl(trimToNull(request.link()));
        String bvid = resolveBvid(request.bvid(), link, request.source());
        if (request.source() == SOURCE_BILIBILI) {
            if (bvid == null && link == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "B 站课程需填写 BV 号或播放链接");
            }
            if (link == null) {
                link = "https://www.bilibili.com/video/" + bvid;
            }
        }
        Long subjectId = validatePrimarySubject(request.subjectId());

        course.setName(request.name().trim());
        course.setCoverUrl(normalizeBilibiliUrl(trimToNull(request.coverUrl())));
        course.setIntro(trimToNull(request.intro()));
        course.setDurationMinutes(request.durationMinutes());
        course.setSource(request.source());
        course.setLink(link);
        course.setBvid(bvid);
        course.setStatus(request.status());
        course.setSubjectId(subjectId);
    }

    /** 非 B 站来源不保留 BV；B 站未填 BV 时尝试从播放链接提取。 */
    private String resolveBvid(String rawBvid, String link, int source) {
        if (source != SOURCE_BILIBILI) {
            return null;
        }
        String bvid = trimToNull(rawBvid);
        if (bvid == null && link != null) {
            bvid = extractBvid(link);
        }
        return bvid;
    }

    static String extractBvid(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = BVID_PATTERN.matcher(text);
        return matcher.find() ? "BV" + matcher.group(1) : null;
    }

    /**
     * B 站图片 / 链接统一升级为 HTTPS。
     *
     * <p>导入数据常为 {@code http://i0.hdslb.com/...}，在 HTTPS 管理站下会被浏览器
     * 按混合内容拦截；升级后同时兼容开发与生产环境。</p>
     */
    static String normalizeBilibiliUrl(String url) {
        String value = trimToNull(url);
        if (value == null) {
            return null;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://")
                && (lower.contains("hdslb.com") || lower.contains("bilibili.com")
                    || lower.contains("b23.tv"))) {
            return "https://" + value.substring(7);
        }
        return value;
    }

    private Long validatePrimarySubject(Long subjectId) {
        if (subjectId == null) {
            return null;
        }
        if (subjectMapper.selectById(subjectId) == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "主学科不存在");
        }
        return subjectId;
    }

    private List<Long> normalizeExtraSubjects(Long primarySubjectId, List<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return List.of();
        }
        if (primarySubjectId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "选择关联学科时必须指定主学科");
        }
        LinkedHashSet<Long> unique = subjectIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.equals(primarySubjectId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (unique.isEmpty()) {
            return List.of();
        }
        Set<Long> existing = subjectMapper.selectBatchIds(unique).stream()
                .map(Subject::getId)
                .collect(Collectors.toSet());
        if (existing.size() != unique.size()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "关联学科不存在");
        }
        return new ArrayList<>(unique);
    }

    private void ensureBvidUnique(String bvid, Long excludeId) {
        if (bvid == null) {
            return;
        }
        var query = Wrappers.<Course>lambdaQuery().eq(Course::getBvid, bvid);
        if (excludeId != null) {
            query.ne(Course::getId, excludeId);
        }
        if (courseMapper.selectCount(query) > 0) {
            throw new BizException(ErrorCode.NAME_EXISTS, "BV 号已存在");
        }
    }

    /** 覆盖式重建额外学科关联（物理删除后插入，避免唯一键被逻辑删除行占用）。 */
    private void replaceSubjects(Long courseId, List<Long> subjectIds) {
        courseSubjectMapper.deleteByCourseId(courseId);
        for (Long subjectId : subjectIds) {
            CourseSubject relation = new CourseSubject();
            relation.setCourseId(courseId);
            relation.setSubjectId(subjectId);
            courseSubjectMapper.insert(relation);
        }
    }

    private List<CourseItem> toItems(List<Course> courses) {
        if (courses.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        Map<Long, List<CourseSubject>> relations = courseSubjectMapper.selectList(
                        Wrappers.<CourseSubject>lambdaQuery()
                                .in(CourseSubject::getCourseId, courseIds)
                                .orderByAsc(CourseSubject::getId)).stream()
                .collect(Collectors.groupingBy(CourseSubject::getCourseId));

        Set<Long> subjectIds = new LinkedHashSet<>();
        for (Course course : courses) {
            if (course.getSubjectId() != null) {
                subjectIds.add(course.getSubjectId());
            }
            relations.getOrDefault(course.getId(), List.of()).stream()
                    .map(CourseSubject::getSubjectId)
                    .forEach(subjectIds::add);
        }
        Map<Long, Subject> subjectMap = subjectIds.isEmpty() ? Map.of()
                : subjectMapper.selectBatchIds(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity(), (a, b) -> a));

        return courses.stream().map(course -> {
            List<SubjectRef> refs = new ArrayList<>();
            Subject primary = course.getSubjectId() == null ? null : subjectMap.get(course.getSubjectId());
            if (primary != null) {
                refs.add(new SubjectRef(primary.getId(), primary.getName()));
            }
            for (CourseSubject relation : relations.getOrDefault(course.getId(), List.of())) {
                Subject subject = subjectMap.get(relation.getSubjectId());
                if (subject != null && !subject.getId().equals(course.getSubjectId())) {
                    refs.add(new SubjectRef(subject.getId(), subject.getName()));
                }
            }
            return new CourseItem(course.getId(), course.getName(),
                    normalizeBilibiliUrl(course.getCoverUrl()),
                    course.getIntro(), course.getDurationMinutes(), course.getSource(),
                    normalizeBilibiliUrl(course.getLink()),
                    course.getBvid(), course.getStatus(), course.getSubjectId(), refs);
        }).toList();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

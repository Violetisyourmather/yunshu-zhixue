package com.yunshu.zhixue.modules.course.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.constant.RedisKeys;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.course.dto.CourseImportIssue;
import com.yunshu.zhixue.modules.course.dto.CourseImportPreview;
import com.yunshu.zhixue.modules.course.dto.CourseImportResult;
import com.yunshu.zhixue.modules.course.dto.CourseImportRow;
import com.yunshu.zhixue.modules.course.entity.Course;
import com.yunshu.zhixue.modules.course.entity.CourseSubject;
import com.yunshu.zhixue.modules.course.mapper.CourseMapper;
import com.yunshu.zhixue.modules.course.mapper.CourseSubjectMapper;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogRecorder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 课程 Excel 批量导入（A-C06 预览 / A-C07 确认），两段式：
 *
 * <p>① 上传 Excel → 按表头解析并逐行校验 → 返回有效 / 警告 / 无效行与预览令牌；
 * ② 确认导入 → 从 Redis 读取预览结果，单事务写入，默认上架。</p>
 *
 * <p>表头：课程名称 / 所属学科 / 来源 / 时长（分钟） / BV号 / 封面 / 播放链接。
 * 「所属学科」支持逗号、顿号分隔多个学科；「其他学科」表示不归属具体学科。</p>
 *
 * <p>A-C09 粘贴文本行复用同一套校验与预览 token：有表头时按列名识别；无表头时默认
 * 「课程名称 | 时长 | 所属学科 | 来源 | 课程链接 | 封面 | BV号」，同时兼容 Excel 列顺序。</p>
 */
@Service
public class CourseImportService {

    private static final int SOURCE_BILIBILI = 1;
    private static final int SOURCE_OTHER = 2;
    private static final String STATUS_VALID = "valid";
    private static final String STATUS_WARNING = "warning";
    private static final String STATUS_INVALID = "invalid";
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_TEXT_CHARS = 1_000_000;
    private static final Set<String> EXCEL_EXTENSIONS = Set.of("xlsx", "xls");
    private static final String OTHER_SUBJECT = "其他学科";
    private static final String BILIBILI_VIDEO_URL = "https://www.bilibili.com/video/";
    private static final Pattern BVID_EXACT = Pattern.compile("(?i)^BV[0-9A-Za-z]{10}$");
    private static final Pattern DURATION_CLOCK = Pattern.compile("^(\\d{1,4})[:：](\\d{1,2})$");
    private static final Pattern DURATION_DIGITS = Pattern.compile("(\\d+)");

    private final CourseMapper courseMapper;
    private final CourseSubjectMapper courseSubjectMapper;
    private final SubjectMapper subjectMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final OperationLogRecorder operationLogRecorder;

    public CourseImportService(CourseMapper courseMapper,
                               CourseSubjectMapper courseSubjectMapper,
                               SubjectMapper subjectMapper,
                               StringRedisTemplate redis,
                               ObjectMapper objectMapper,
                               OperationLogRecorder operationLogRecorder) {
        this.courseMapper = courseMapper;
        this.courseSubjectMapper = courseSubjectMapper;
        this.subjectMapper = subjectMapper;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.operationLogRecorder = operationLogRecorder;
    }

    /** A-C06 上传 Excel 并返回预览（不入库）。 */
    public CourseImportPreview preview(MultipartFile file) {
        validateFile(file);
        List<Map<Integer, String>> rawRows = readRows(file);
        if (rawRows.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "Excel 中没有数据");
        }

        Columns columns = resolveColumns(rawRows.get(0));
        return buildPreview(rawRows.subList(1, rawRows.size()), columns, "Excel", 2);
    }

    /**
     * A-C09 粘贴文本行并返回预览（不入库）。
     *
     * <p>每行一门课程，字段用 Tab、竖线或逗号分隔，顺序为：
     * 课程名称 / 所属学科 / 来源 / 时长（分钟） / BV号 / 封面 / 播放链接。
     * 第一行可带表头，表头别名与 Excel 导入一致；空行、{@code #} 与 {@code //} 开头的行会被忽略。</p>
     */
    public CourseImportPreview previewText(String text) {
        if (text == null || text.isBlank()) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "请粘贴课程文本");
        }
        text = text.replace("\uFEFF", "");
        if (text.length() > MAX_TEXT_CHARS) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "粘贴内容不能超过 100 万个字符");
        }

        List<Map<Integer, String>> rawRows = new ArrayList<>();
        boolean hasHeader = false;
        for (String line : text.split("\\R", -1)) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) {
                continue;
            }
            Map<Integer, String> row = parseTextLine(trimmed);
            if (rawRows.isEmpty() && looksLikeHeader(row)) {
                hasHeader = true;
            }
            rawRows.add(row);
        }
        if (rawRows.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "文本中没有可解析的课程行");
        }

        Columns columns = hasHeader
                ? resolveColumns(rawRows.get(0))
                : resolveTextColumns(rawRows.get(0));
        List<Map<Integer, String>> dataRows = hasHeader
                ? rawRows.subList(1, rawRows.size())
                : rawRows;
        return buildPreview(dataRows, columns, "文本", hasHeader ? 2 : 1);
    }

    private CourseImportPreview buildPreview(List<Map<Integer, String>> dataRows,
                                             Columns columns,
                                             String sourceLabel,
                                             int firstDataRow) {
        if (dataRows.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_INVALID,
                    sourceLabel + "中没有课程数据行");
        }
        if (dataRows.size() > AppConstants.IMPORT_MAX_ROWS) {
            throw new BizException(ErrorCode.IMPORT_INVALID,
                    "单次最多导入 " + AppConstants.IMPORT_MAX_ROWS + " 行课程");
        }

        Map<String, Long> subjectIds = subjectMapper.selectList(null).stream()
                .collect(Collectors.toMap(Subject::getName, Subject::getId, (a, b) -> a));

        List<RowBuilder> builders = new ArrayList<>();
        Map<String, Integer> seenBvids = new LinkedHashMap<>();
        for (int index = 0; index < dataRows.size(); index++) {
            int rowNumber = index + firstDataRow;
            builders.add(parseRow(rowNumber, dataRows.get(index), columns, subjectIds, seenBvids));
        }
        markExistingBvids(builders);

        List<CourseImportRow> rows = builders.stream().map(RowBuilder::toRow).toList();
        long validCount = rows.stream().filter(row -> STATUS_VALID.equals(row.status())).count();
        long warningCount = rows.stream().filter(row -> STATUS_WARNING.equals(row.status())).count();
        long invalidCount = rows.stream().filter(row -> STATUS_INVALID.equals(row.status())).count();

        String token = null;
        if (validCount + warningCount > 0) {
            token = UUID.randomUUID().toString().replace("-", "");
            try {
                String json = objectMapper.writeValueAsString(rows);
                redis.opsForValue().set(RedisKeys.COURSE_IMPORT_PREVIEW + token, json,
                        Duration.ofMinutes(AppConstants.IMPORT_PREVIEW_TTL_MINUTES));
            } catch (Exception ex) {
                throw new BizException(ErrorCode.IMPORT_INVALID, "导入预览保存失败，请重试");
            }
        }
        return new CourseImportPreview(token, rows.size(), (int) validCount,
                (int) warningCount, (int) invalidCount, rows);
    }

    /** 文本行按 Tab → 竖线 → 逗号的优先级拆分，最多 7 列；最后一列保留剩余分隔符。 */
    private static Map<Integer, String> parseTextLine(String line) {
        String[] cells;
        if (line.indexOf('\t') >= 0) {
            cells = line.split("\\t", 7);
        } else if (line.indexOf('|') >= 0 || line.indexOf('｜') >= 0) {
            cells = line.split("[|｜]", 7);
        } else {
            cells = line.split("[,，]", 7);
        }
        Map<Integer, String> row = new LinkedHashMap<>();
        for (int index = 0; index < cells.length; index++) {
            row.put(index, cells[index].strip());
        }
        return row;
    }

    /** 第一行是否表头：包含「课程名称」或「课程名」即视为表头。 */
    private static boolean looksLikeHeader(Map<Integer, String> row) {
        return row.values().stream()
                .map(CourseImportService::normalizeHeader)
                .anyMatch(value -> value.contains("课程名称") || value.contains("课程名"));
    }

    /**
     * 无表头文本的列顺序：
     * 默认按需求 4.3.2 的「课程名称 | 时长 | 学科 | 来源 | 课程链接 | 封面 | BV号」；
     * 若第二列不是时长、第四列是时长，则兼容 Excel 的「名称 | 学科 | 来源 | 时长 | BV | 封面 | 链接」。
     */
    private static Columns resolveTextColumns(Map<Integer, String> firstRow) {
        Integer secondAsDuration = parseDuration(cell(firstRow, 1));
        Integer fourthAsDuration = parseDuration(cell(firstRow, 3));
        if (secondAsDuration == null && fourthAsDuration != null) {
            return new Columns(0, 3, 1, 2, 4, 5, 6);
        }
        return new Columns(0, 1, 2, 3, 6, 5, 4);
    }

    /** A-C07 确认导入：读取预览令牌，单事务写入有效行与警告行，默认上架。 */
    @Transactional
    public CourseImportResult confirm(String token) {
        String key = RedisKeys.COURSE_IMPORT_PREVIEW + token;
        String json = redis.opsForValue().get(key);
        if (json == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "导入预览已失效，请重新上传并解析");
        }

        List<CourseImportRow> rows;
        try {
            rows = objectMapper.readValue(json, new TypeReference<List<CourseImportRow>>() {
            });
        } catch (Exception ex) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "导入数据已损坏，请重新解析");
        }
        List<CourseImportRow> importable = rows.stream()
                .filter(row -> !STATUS_INVALID.equals(row.status()))
                .toList();
        if (importable.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "没有可导入的有效行");
        }

        try {
            for (CourseImportRow row : importable) {
                insertRow(row);
            }
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.NAME_EXISTS, "BV 号已存在，导入已取消，请重新解析");
        }

        redis.delete(key);
        operationLogRecorder.record(OperationActions.CREATE, "课程",
                "批量导入 " + importable.size() + " 门课程");
        return new CourseImportResult(importable.size());
    }

    private void insertRow(CourseImportRow row) {
        Course course = new Course();
        course.setName(row.name());
        course.setCoverUrl(CourseService.normalizeBilibiliUrl(row.coverUrl()));
        course.setDurationMinutes(row.durationMinutes());
        course.setSource(row.source());
        course.setLink(CourseService.normalizeBilibiliUrl(row.link()));
        course.setBvid(row.bvid());
        course.setStatus(1);
        course.setSubjectId(row.subjectId());
        courseMapper.insert(course);

        if (row.subjectIds() == null) {
            return;
        }
        for (Long subjectId : row.subjectIds()) {
            CourseSubject relation = new CourseSubject();
            relation.setCourseId(course.getId());
            relation.setSubjectId(subjectId);
            courseSubjectMapper.insert(relation);
        }
    }

    private RowBuilder parseRow(int excelRow, Map<Integer, String> row, Columns columns,
                                Map<String, Long> subjectIds, Map<String, Integer> seenBvids) {
        RowBuilder builder = new RowBuilder(excelRow);

        String name = cell(row, columns.name());
        if (name == null) {
            builder.error("课程名称", "不能为空");
        } else if (name.length() > 100) {
            builder.error("课程名称", "不能超过 100 个字符");
        }

        Integer duration = parseDuration(cell(row, columns.duration()));
        if (duration == null) {
            builder.error("时长", "请填写时长（分钟），如 293");
        } else if (duration <= 0) {
            builder.error("时长", "必须大于 0 分钟");
        } else if (duration > 100000) {
            builder.error("时长", "不能超过 100000 分钟");
        }

        String subjectRaw = cell(row, columns.subject());
        SubjectResolution resolution = resolveSubjects(subjectRaw, subjectIds, builder);

        int source = resolveSource(cell(row, columns.source()), builder);

        String link = CourseService.normalizeBilibiliUrl(cell(row, columns.link()));
        if (link != null && link.length() > 500) {
            builder.error("播放链接", "不能超过 500 个字符");
        }

        String bvid = null;
        if (source == SOURCE_BILIBILI) {
            bvid = cell(row, columns.bvid());
            if (bvid == null && link != null) {
                bvid = CourseService.extractBvid(link);
            }
            if (bvid != null && bvid.length() > 20) {
                builder.error("BV号", "不能超过 20 个字符");
            } else if (bvid != null && !BVID_EXACT.matcher(bvid).matches()) {
                builder.warn("BV号", "格式可能不正确：" + bvid);
            }
            if (bvid == null && link == null) {
                builder.error("播放链接", "B 站课程需填写 BV 号或播放链接");
            }
            if (link == null && bvid != null) {
                link = BILIBILI_VIDEO_URL + bvid;
            }
        }
        if (link == null) {
            builder.warn("播放链接", "未填写播放链接");
        }

        if (bvid != null) {
            String key = bvid.toUpperCase(Locale.ROOT);
            Integer firstRow = seenBvids.putIfAbsent(key, excelRow);
            if (firstRow != null) {
                builder.error("BV号", "文件内重复（首次出现于第 " + firstRow + " 行）");
            }
        }

        String coverUrl = CourseService.normalizeBilibiliUrl(cell(row, columns.cover()));
        if (coverUrl != null && coverUrl.length() > 500) {
            builder.error("封面", "不能超过 500 个字符");
        }

        builder.name = name == null ? "" : name;
        builder.durationMinutes = duration;
        builder.subjectNames = subjectRaw == null ? OTHER_SUBJECT : subjectRaw;
        builder.subjectId = resolution.primary();
        builder.subjectIds = resolution.extra();
        builder.source = source;
        builder.bvid = bvid;
        builder.coverUrl = coverUrl;
        builder.link = link;
        return builder;
    }

    private SubjectResolution resolveSubjects(String raw, Map<String, Long> subjectIds, RowBuilder builder) {
        if (raw == null) {
            builder.warn("所属学科", "未填写学科，按「其他学科」导入");
            return new SubjectResolution(null, List.of());
        }
        if (raw.startsWith(OTHER_SUBJECT)) {
            return new SubjectResolution(null, List.of());
        }

        List<Long> ids = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        boolean hasOther = false;
        for (String part : raw.split("[,，、;；]")) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            if (token.startsWith(OTHER_SUBJECT)) {
                hasOther = true;
                continue;
            }
            Long subjectId = subjectIds.get(token);
            if (subjectId == null) {
                unknown.add(token);
            } else if (!ids.contains(subjectId)) {
                ids.add(subjectId);
            }
        }
        if (!unknown.isEmpty()) {
            builder.warn("所属学科", "未找到学科：" + String.join("、", unknown) + "，已忽略");
        }
        if (hasOther && !ids.isEmpty()) {
            builder.warn("所属学科", "「其他学科」与具体学科同时填写，已按具体学科处理");
        }
        if (ids.isEmpty()) {
            return new SubjectResolution(null, List.of());
        }
        return new SubjectResolution(ids.get(0), ids.subList(1, ids.size()));
    }

    private int resolveSource(String raw, RowBuilder builder) {
        if (raw == null) {
            builder.warn("来源", "未填写来源，默认按 B站 导入");
            return SOURCE_BILIBILI;
        }
        String normalized = raw.replaceAll("\\s+", "").replace("　", "").toLowerCase(Locale.ROOT);
        if (normalized.contains("b站") || normalized.contains("bilibili") || normalized.contains("bili")) {
            return SOURCE_BILIBILI;
        }
        if (normalized.contains("其他")) {
            return SOURCE_OTHER;
        }
        builder.warn("来源", "来源「" + raw + "」无法识别，默认按 B站 导入");
        return SOURCE_BILIBILI;
    }

    /** 预览阶段一次性校验 BV 号在库内是否已存在。 */
    private void markExistingBvids(List<RowBuilder> builders) {
        List<String> bvids = builders.stream()
                .map(builder -> builder.bvid)
                .filter(value -> value != null)
                .distinct()
                .toList();
        if (bvids.isEmpty()) {
            return;
        }
        Set<String> existing = courseMapper.selectList(Wrappers.<Course>lambdaQuery()
                        .select(Course::getBvid)
                        .in(Course::getBvid, bvids)).stream()
                .map(course -> course.getBvid() == null ? "" : course.getBvid().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        for (RowBuilder builder : builders) {
            if (builder.bvid != null
                    && existing.contains(builder.bvid.toUpperCase(Locale.ROOT))) {
                builder.error("BV号", "系统中已存在该 BV 号");
            }
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "请选择要导入的 Excel 文件");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BizException(ErrorCode.FILE_INVALID, "Excel 文件大小不能超过 10MB");
        }
        String filename = file.getOriginalFilename();
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!EXCEL_EXTENSIONS.contains(extension)) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "仅支持 .xlsx / .xls 文件");
        }
    }

    private List<Map<Integer, String>> readRows(MultipartFile file) {
        List<Map<Integer, String>> rows = new ArrayList<>();
        try (InputStream input = file.getInputStream()) {
            EasyExcel.read(input)
                    .sheet()
                    .headRowNumber(0)
                    .registerReadListener(new AnalysisEventListener<Map<Integer, String>>() {
                        @Override
                        public void invoke(Map<Integer, String> data, AnalysisContext context) {
                            rows.add(new LinkedHashMap<>(data));
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                            // 全部行已在 invoke 中收集
                        }
                    })
                    .doRead();
        } catch (Exception ex) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "Excel 解析失败，请检查文件格式");
        }
        return rows;
    }

    private Columns resolveColumns(Map<Integer, String> header) {
        Integer name = findColumn(header, "课程名称", "课程名", "名称", "标题");
        Integer duration = findColumn(header, "时长", "分钟", "duration");
        if (name == null) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "缺少表头：课程名称");
        }
        if (duration == null) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "缺少表头：时长（分钟）");
        }
        return new Columns(
                name,
                duration,
                findColumn(header, "所属学科", "学科", "关联学科"),
                findColumn(header, "来源", "课程来源"),
                findColumn(header, "bv号", "bvid", "bv"),
                findColumn(header, "封面", "封面图片", "封面url"),
                findColumn(header, "播放链接", "课程链接", "链接", "播放地址"));
    }

    private Integer findColumn(Map<Integer, String> header, String... aliases) {
        for (Map.Entry<Integer, String> entry : header.entrySet()) {
            String value = normalizeHeader(entry.getValue());
            if (value.isEmpty()) {
                continue;
            }
            for (String alias : aliases) {
                if (value.contains(alias)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private static String normalizeHeader(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace(' ', ' ')
                .replace('　', ' ')
                .replaceAll("\\s+", "")
                .replace('（', '(')
                .replace('）', ')')
                .toLowerCase(Locale.ROOT);
    }

    private static String cell(Map<Integer, String> row, Integer column) {
        if (column == null) {
            return null;
        }
        String value = row.get(column);
        if (value == null) {
            return null;
        }
        String cleaned = value.replace(' ', ' ').trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static Integer parseDuration(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher clock = DURATION_CLOCK.matcher(raw);
        if (clock.matches()) {
            int hours = Integer.parseInt(clock.group(1));
            int minutes = Integer.parseInt(clock.group(2));
            if (minutes > 59) {
                return null;
            }
            return hours * 60 + minutes;
        }
        Matcher digits = DURATION_DIGITS.matcher(raw);
        if (!digits.find()) {
            return null;
        }
        try {
            return Integer.parseInt(digits.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record Columns(Integer name, Integer duration, Integer subject, Integer source,
                           Integer bvid, Integer cover, Integer link) {
    }

    private record SubjectResolution(Long primary, List<Long> extra) {
    }

    private static final class RowBuilder {

        private final int row;
        private final List<CourseImportIssue> errors = new ArrayList<>();
        private final List<CourseImportIssue> warnings = new ArrayList<>();
        private String name;
        private Integer durationMinutes;
        private String subjectNames;
        private Long subjectId;
        private List<Long> subjectIds = List.of();
        private Integer source;
        private String bvid;
        private String coverUrl;
        private String link;

        private RowBuilder(int row) {
            this.row = row;
        }

        private void error(String field, String reason) {
            errors.add(new CourseImportIssue(field, reason));
        }

        private void warn(String field, String reason) {
            warnings.add(new CourseImportIssue(field, reason));
        }

        private CourseImportRow toRow() {
            String status;
            if (!errors.isEmpty()) {
                status = STATUS_INVALID;
            } else if (!warnings.isEmpty()) {
                status = STATUS_WARNING;
            } else {
                status = STATUS_VALID;
            }
            return new CourseImportRow(row, name, durationMinutes, subjectNames, subjectId,
                    subjectIds == null ? List.of() : subjectIds, source, bvid, coverUrl, link,
                    status, List.copyOf(errors), List.copyOf(warnings));
        }
    }
}

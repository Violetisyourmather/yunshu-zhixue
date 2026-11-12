package com.yunshu.zhixue.modules.interviewbank.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.constant.RedisKeys;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportIssue;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportPayload;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportPreview;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportResult;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportRow;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewMarkdownConfirmRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewMarkdownPayload;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewMarkdownPreview;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewMarkdownQuestion;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewMarkdownResult;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewMarkdownSample;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewQuestion;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewTopic;
import com.yunshu.zhixue.modules.interviewbank.mapper.InterviewQuestionMapper;
import com.yunshu.zhixue.modules.interviewbank.mapper.InterviewTopicMapper;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogRecorder;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 面试题库 Excel 导入。
 *
 * <p>表头：所属学科 / 题干 / 难度 / 主题 / 标签 / 参考答案。
 * 面试题按每行的「所属学科」归属方向，不需要额外选择方向；主题不存在时确认导入后自动补入词表。
 * 导入来源记为「手动导入」，文件名与行号写入来源信息。</p>
 */
@Service
public class InterviewImportService {

    private static final String STATUS_VALID = "valid";
    private static final String STATUS_WARNING = "warning";
    private static final String STATUS_INVALID = "invalid";
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> EXCEL_EXTENSIONS = Set.of("xlsx", "xls");
    private static final Set<String> MARKDOWN_EXTENSIONS = Set.of("md", "markdown");
    private static final long MAX_MARKDOWN_BYTES = 2L * 1024 * 1024;
    private static final int MAX_MARKDOWN_QUESTIONS = 500;
    private static final int MARKDOWN_SAMPLE_LIMIT = 3;
    private static final int MARKDOWN_ANSWER_PREVIEW = 80;
    /** Markdown 中 # ~ ### 标题切题，标题为题、正文为参考答案。 */
    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^#{1,3}\\s+(.+?)\\s*$");
    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final InterviewQuestionMapper questionMapper;
    private final InterviewTopicMapper topicMapper;
    private final SubjectMapper subjectMapper;
    private final InterviewQuestionService questionService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final OperationLogRecorder operationLogRecorder;

    public InterviewImportService(InterviewQuestionMapper questionMapper,
                                  InterviewTopicMapper topicMapper,
                                  SubjectMapper subjectMapper,
                                  InterviewQuestionService questionService,
                                  StringRedisTemplate redis,
                                  ObjectMapper objectMapper,
                                  OperationLogRecorder operationLogRecorder) {
        this.questionMapper = questionMapper;
        this.topicMapper = topicMapper;
        this.subjectMapper = subjectMapper;
        this.questionService = questionService;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.operationLogRecorder = operationLogRecorder;
    }

    /** 解析 Excel 并返回预览（不入库）。 */
    public InterviewImportPreview preview(MultipartFile file) {
        validateFile(file);
        Map<String, Long> subjects = subjectMapper.selectList(null).stream()
                .collect(Collectors.toMap(Subject::getName, Subject::getId, (a, b) -> a));
        Set<String> topicNames = topicMapper.selectList(null).stream()
                .map(InterviewTopic::getName)
                .collect(Collectors.toSet());
        String fileName = file.getOriginalFilename() == null ? "面试题导入.xlsx" : file.getOriginalFilename();

        List<InterviewImportRow> rows = readRows(file, subjects, topicNames);
        if (rows.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "Excel 中没有可导入的面试题数据");
        }
        int validCount = (int) rows.stream().filter(row -> STATUS_VALID.equals(row.status())).count();
        int warningCount = (int) rows.stream().filter(row -> STATUS_WARNING.equals(row.status())).count();
        int invalidCount = (int) rows.stream().filter(row -> STATUS_INVALID.equals(row.status())).count();
        List<String> newTopics = rows.stream()
                .filter(row -> !STATUS_INVALID.equals(row.status()))
                .filter(InterviewImportRow::newTopic)
                .map(InterviewImportRow::topic)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        String token = null;
        if (validCount + warningCount > 0) {
            token = UUID.randomUUID().toString().replace("-", "");
            try {
                String json = objectMapper.writeValueAsString(
                        new InterviewImportPayload(fileName, rows));
                redis.opsForValue().set(RedisKeys.INTERVIEW_IMPORT_PREVIEW + token, json,
                        Duration.ofMinutes(AppConstants.IMPORT_PREVIEW_TTL_MINUTES));
            } catch (Exception ex) {
                throw new BizException(ErrorCode.IMPORT_INVALID, "导入预览保存失败，请重试");
            }
        }
        return new InterviewImportPreview(token, fileName, rows.size(), validCount,
                warningCount, invalidCount, newTopics, rows);
    }

    /** 确认导入：补全主题词表，题目统一上架、来源为手动导入。 */
    @Transactional
    public InterviewImportResult confirm(String token) {
        String key = RedisKeys.INTERVIEW_IMPORT_PREVIEW + token;
        String json = redis.opsForValue().get(key);
        if (json == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "导入预览已失效，请重新上传并解析");
        }
        InterviewImportPayload payload;
        try {
            payload = objectMapper.readValue(json, InterviewImportPayload.class);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "导入数据已损坏，请重新解析");
        }
        List<InterviewImportRow> importable = payload.rows().stream()
                .filter(row -> !STATUS_INVALID.equals(row.status()))
                .toList();
        if (importable.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "没有可导入的有效行");
        }

        Set<String> newTopics = importable.stream()
                .filter(InterviewImportRow::newTopic)
                .map(InterviewImportRow::topic)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (String topic : newTopics) {
            questionService.ensureTopic(topic);
        }

        int imported = 0;
        for (InterviewImportRow row : importable) {
            InterviewQuestion question = new InterviewQuestion();
            question.setSubjectId(row.subjectId());
            question.setContent(row.content());
            question.setReferenceAnswer(row.referenceAnswer());
            question.setTopic(row.topic());
            question.setDifficulty(row.difficulty());
            question.setTags(row.tags());
            question.setSourceType(3);
            question.setSourceDocName(payload.fileName());
            question.setSourceSeq(row.row());
            question.setStatus(1);
            questionMapper.insert(question);
            imported++;
        }
        redis.delete(key);
        operationLogRecorder.record(OperationActions.CREATE, "面试题",
                "批量导入 " + imported + " 道题");
        return new InterviewImportResult(imported, newTopics.size());
    }

    /**
     * Markdown 导入预览：按 # ~ ### 标题切题，标题为题干、正文为参考答案。
     *
     * <p>解析结果暂存 Redis（TTL 30min），确认时再统一指定方向、主题、难度与标签。</p>
     */
    public InterviewMarkdownPreview previewMarkdown(MultipartFile file) {
        validateMarkdownFile(file);
        String text = readMarkdown(file);
        List<InterviewMarkdownQuestion> questions = parseMarkdown(text);
        if (questions.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_INVALID,
                    "Markdown 中没有可导入的面试题，请使用 # ~ ### 标题作为题干");
        }
        if (questions.size() > MAX_MARKDOWN_QUESTIONS) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "单次最多导入 500 道面试题");
        }

        String fileName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "面试题导入.md";
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            String json = objectMapper.writeValueAsString(
                    new InterviewMarkdownPayload(fileName, questions));
            redis.opsForValue().set(RedisKeys.INTERVIEW_IMPORT_MARKDOWN + token, json,
                    Duration.ofMinutes(AppConstants.IMPORT_PREVIEW_TTL_MINUTES));
        } catch (Exception ex) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "导入预览保存失败，请重试");
        }

        List<InterviewMarkdownSample> samples = questions.stream()
                .limit(MARKDOWN_SAMPLE_LIMIT)
                .map(item -> new InterviewMarkdownSample(item.seq(), item.title(),
                        previewAnswer(item.answer()), item.line()))
                .toList();
        return new InterviewMarkdownPreview(token, fileName, file.getSize(),
                questions.size(), samples);
    }

    /** 确认导入 Markdown：整批统一指定方向、主题、难度、标签，来源记为手动导入。 */
    @Transactional
    public InterviewMarkdownResult confirmMarkdown(InterviewMarkdownConfirmRequest request) {
        String key = RedisKeys.INTERVIEW_IMPORT_MARKDOWN + request.token();
        String json = redis.opsForValue().get(key);
        if (json == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "导入预览已失效，请重新上传并解析");
        }
        InterviewMarkdownPayload payload;
        try {
            payload = objectMapper.readValue(json, InterviewMarkdownPayload.class);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "导入数据已损坏，请重新解析");
        }
        if (payload.questions() == null || payload.questions().isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "没有可导入的面试题");
        }
        Subject subject = subjectMapper.selectById(request.subjectId());
        if (subject == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "面试方向不存在");
        }

        int difficulty = request.difficulty() == null ? 2 : request.difficulty();
        String topic = trimToNull(request.topic());
        String tags = trimToNull(request.tags());
        if (topic != null) {
            questionService.ensureTopic(topic);
        }

        int imported = 0;
        for (InterviewMarkdownQuestion item : payload.questions()) {
            InterviewQuestion question = new InterviewQuestion();
            question.setSubjectId(subject.getId());
            question.setContent(item.title());
            question.setReferenceAnswer(item.answer());
            question.setTopic(topic);
            question.setDifficulty(difficulty);
            question.setTags(tags);
            question.setSourceType(3);
            question.setSourceDocName(payload.fileName());
            question.setSourceSeq(item.seq());
            question.setStatus(1);
            questionMapper.insert(question);
            imported++;
        }
        redis.delete(key);
        operationLogRecorder.record(OperationActions.CREATE, "面试题",
                "Markdown 导入 " + imported + " 道题");
        return new InterviewMarkdownResult(imported);
    }

    private String readMarkdown(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "Markdown 文件读取失败");
        }
    }

    private List<InterviewMarkdownQuestion> parseMarkdown(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.startsWith("﻿")) {
            normalized = normalized.substring(1);
        }
        String[] lines = normalized.split("\n", -1);
        List<InterviewMarkdownQuestion> questions = new ArrayList<>();
        String title = null;
        int titleLine = 0;
        StringBuilder answer = new StringBuilder();
        for (int index = 0; index < lines.length; index++) {
            Matcher matcher = MARKDOWN_HEADING.matcher(lines[index]);
            if (matcher.matches()) {
                addMarkdownQuestion(questions, title, answer, titleLine);
                title = matcher.group(1).trim();
                titleLine = index + 1;
                answer.setLength(0);
            } else if (title != null) {
                answer.append(lines[index]).append('\n');
            }
        }
        addMarkdownQuestion(questions, title, answer, titleLine);
        return questions;
    }

    private void addMarkdownQuestion(List<InterviewMarkdownQuestion> questions, String title,
                                     StringBuilder answer, int line) {
        if (!StringUtils.hasText(title)) {
            return;
        }
        String content = title.trim();
        if (content.length() > 10000) {
            throw new BizException(ErrorCode.IMPORT_INVALID,
                    "第 " + line + " 行题干超过 10000 个字符");
        }
        String referenceAnswer = answer.toString().trim();
        if (referenceAnswer.length() > 10000) {
            throw new BizException(ErrorCode.IMPORT_INVALID,
                    "第 " + line + " 行参考答案超过 10000 个字符");
        }
        questions.add(new InterviewMarkdownQuestion(questions.size() + 1, content, referenceAnswer, line));
    }

    private String previewAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return "（无参考答案）";
        }
        String value = answer.replaceAll("\\s+", " ").trim();
        return value.length() <= MARKDOWN_ANSWER_PREVIEW
                ? value
                : value.substring(0, MARKDOWN_ANSWER_PREVIEW) + '…';
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void validateMarkdownFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "请选择要导入的 Markdown 文件");
        }
        if (file.getSize() > MAX_MARKDOWN_BYTES) {
            throw new BizException(ErrorCode.FILE_INVALID, "Markdown 文件大小不能超过 2MB");
        }
        String filename = file.getOriginalFilename();
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!MARKDOWN_EXTENSIONS.contains(extension)) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "仅支持 .md / .markdown 文件");
        }
    }

    /** 下载导入模板（含一行示例）。 */
    public void template(HttpServletResponse response) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of(
                "AI应用与智能体开发",
                "示例：ollama 是什么，有什么作用？",
                "中等",
                "大模型与本地部署",
                "Ollama,本地部署",
                "Ollama 是一个轻量级的大模型本地部署工具。"));
        List<List<String>> head = List.of(
                List.of("所属学科"), List.of("题干"), List.of("难度"),
                List.of("主题"), List.of("标签"), List.of("参考答案"));
        try {
            response.setContentType(CONTENT_TYPE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                    + URLEncoder.encode("面试题库导入模板.xlsx", StandardCharsets.UTF_8)
                    .replace("+", "%20"));
            EasyExcel.write(response.getOutputStream())
                    .head(head)
                    .sheet("面试题库")
                    .doWrite(rows);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "文件生成失败");
        }
    }

    private List<InterviewImportRow> readRows(MultipartFile file, Map<String, Long> subjects,
                                              Set<String> topicNames) {
        QuestionSheetListener listener = new QuestionSheetListener(subjects, topicNames);
        try (InputStream input = file.getInputStream()) {
            EasyExcel.read(input)
                    .headRowNumber(0)
                    .registerReadListener(listener)
                    .doReadAll();
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            Throwable cause = ex;
            while (cause != null) {
                if (cause instanceof BizException bizException) {
                    throw bizException;
                }
                cause = cause.getCause();
            }
            throw new BizException(ErrorCode.IMPORT_INVALID, "Excel 解析失败，请检查文件格式");
        }
        return listener.getRows();
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

    private final class QuestionSheetListener extends AnalysisEventListener<Map<Integer, String>> {

        private final Map<String, Long> subjects;
        private final Set<String> topicNames;
        private final List<InterviewImportRow> rows = new ArrayList<>();
        private final Map<Integer, Columns> columnsBySheet = new HashMap<>();
        private int dataRowCount = 0;

        private QuestionSheetListener(Map<String, Long> subjects, Set<String> topicNames) {
            this.subjects = subjects;
            this.topicNames = topicNames;
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            int sheetNo = context.readSheetHolder().getSheetNo();
            String sheetName = context.readSheetHolder().getSheetName();
            if (!columnsBySheet.containsKey(sheetNo)) {
                columnsBySheet.put(sheetNo, resolveColumns(data, sheetName));
                return;
            }
            Columns columns = columnsBySheet.get(sheetNo);
            if (columns == null) {
                return;
            }
            if (dataRowCount >= AppConstants.IMPORT_MAX_ROWS) {
                throw new BizException(ErrorCode.IMPORT_INVALID,
                        "单次最多导入 " + AppConstants.IMPORT_MAX_ROWS + " 道题");
            }
            dataRowCount++;
            int rowNumber = context.readSheetHolder().getRowIndex() + 1;
            rows.add(parseRow(sheetNo, sheetName, rowNumber, data, columns, subjects, topicNames));
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // 所有行已在 invoke 中收集
        }

        private List<InterviewImportRow> getRows() {
            return rows;
        }
    }

    private Columns resolveColumns(Map<Integer, String> header, String sheetName) {
        Integer content = findColumn(header, "题干", "题目");
        if (content == null) {
            return null;
        }
        Integer subject = findColumn(header, "所属学科", "方向", "学科");
        Integer difficulty = findColumn(header, "难度");
        List<String> missing = new ArrayList<>();
        if (subject == null) {
            missing.add("所属学科");
        }
        if (difficulty == null) {
            missing.add("难度");
        }
        if (!missing.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_INVALID,
                    "工作表「" + sheetName + "」缺少表头：" + String.join("、", missing));
        }
        return new Columns(content, subject, difficulty,
                findColumn(header, "主题", "知识点"),
                findColumn(header, "标签"),
                findColumn(header, "参考答案", "答案"));
    }

    private InterviewImportRow parseRow(int sheetNo, String sheetName, int rowNumber,
                                        Map<Integer, String> data, Columns columns,
                                        Map<String, Long> subjects, Set<String> topicNames) {
        RowBuilder builder = new RowBuilder(sheetNo, sheetName, rowNumber);

        String subjectName = cell(data, columns.subject);
        builder.subjectName = subjectName;
        if (subjectName == null) {
            builder.error("所属学科", "不能为空");
        } else {
            Long subjectId = subjects.get(subjectName);
            if (subjectId == null) {
                builder.error("所属学科", "未找到学科：" + subjectName);
            } else {
                builder.subjectId = subjectId;
            }
        }

        builder.content = cell(data, columns.content);
        if (builder.content == null) {
            builder.error("题干", "不能为空");
        } else if (builder.content.length() > 10000) {
            builder.error("题干", "不能超过 10000 个字符");
        }

        String difficultyRaw = cell(data, columns.difficulty);
        builder.difficulty = parseDifficulty(difficultyRaw);
        if (difficultyRaw == null) {
            builder.difficulty = 2;
            builder.warning("难度", "未填写难度，默认按「中等」导入");
        } else if (builder.difficulty == null) {
            builder.error("难度", "仅支持简单 / 中等 / 困难");
        }

        String topic = cell(data, columns.topic);
        if (topic != null && topic.length() > 50) {
            builder.error("主题", "不能超过 50 个字符");
        }
        builder.topic = topic;
        builder.newTopic = topic != null && !topicNames.contains(topic);

        builder.tags = cell(data, columns.tags);
        if (builder.tags != null && builder.tags.length() > 255) {
            builder.error("标签", "不能超过 255 个字符");
        }

        builder.referenceAnswer = cell(data, columns.referenceAnswer);
        if (builder.referenceAnswer != null && builder.referenceAnswer.length() > 10000) {
            builder.error("参考答案", "不能超过 10000 个字符");
        }
        return builder.toRow();
    }

    private static Integer parseDifficulty(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.replaceAll("\\s+", "");
        if (value.contains("简单") || value.contains("容易")) {
            return 1;
        }
        if (value.contains("中等") || value.contains("一般")) {
            return 2;
        }
        if (value.contains("困难") || value.contains("较难") || value.contains("难")) {
            return 3;
        }
        return null;
    }

    private static Integer findColumn(Map<Integer, String> header, String... aliases) {
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

    private record Columns(Integer content, Integer subject, Integer difficulty,
                           Integer topic, Integer tags, Integer referenceAnswer) {
    }

    private static final class RowBuilder {

        private final int sheetNo;
        private final String sheetName;
        private final int row;
        private final List<InterviewImportIssue> errors = new ArrayList<>();
        private final List<InterviewImportIssue> warnings = new ArrayList<>();
        private String subjectName;
        private Long subjectId;
        private String content;
        private Integer difficulty;
        private String topic;
        private boolean newTopic;
        private String tags;
        private String referenceAnswer;

        private RowBuilder(int sheetNo, String sheetName, int row) {
            this.sheetNo = sheetNo;
            this.sheetName = sheetName;
            this.row = row;
        }

        private void error(String field, String reason) {
            errors.add(new InterviewImportIssue(field, reason));
        }

        private void warning(String field, String reason) {
            warnings.add(new InterviewImportIssue(field, reason));
        }

        private InterviewImportRow toRow() {
            String status;
            if (!errors.isEmpty()) {
                status = STATUS_INVALID;
            } else if (!warnings.isEmpty()) {
                status = STATUS_WARNING;
            } else {
                status = STATUS_VALID;
            }
            return new InterviewImportRow(sheetNo, sheetName, row, subjectName, subjectId,
                    content, difficulty, topic, newTopic, tags, referenceAnswer,
                    status, List.copyOf(errors), List.copyOf(warnings));
        }
    }
}

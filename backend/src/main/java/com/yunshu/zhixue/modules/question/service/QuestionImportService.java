package com.yunshu.zhixue.modules.question.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.constant.RedisKeys;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.question.dto.QuestionImportIssue;
import com.yunshu.zhixue.modules.question.dto.QuestionImportOption;
import com.yunshu.zhixue.modules.question.dto.QuestionImportPayload;
import com.yunshu.zhixue.modules.question.dto.QuestionImportPreview;
import com.yunshu.zhixue.modules.question.dto.QuestionImportResult;
import com.yunshu.zhixue.modules.question.dto.QuestionImportRow;
import com.yunshu.zhixue.modules.question.entity.Question;
import com.yunshu.zhixue.modules.question.entity.QuestionOption;
import com.yunshu.zhixue.modules.question.mapper.QuestionMapper;
import com.yunshu.zhixue.modules.question.mapper.QuestionOptionMapper;
import com.yunshu.zhixue.modules.subject.entity.Stage;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.StageMapper;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import com.yunshu.zhixue.modules.system.service.OperationLogRecorder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * 题库 Excel 导入（A-Q12 预览 / A-Q13 确认）。
 *
 * <p>导入文件可包含多个工作表，表头：
 * 题干 / 分类 / 题型 / 难度 / 选项A~F / 正确答案 / 解析 / 标签。
 * 「分类」作为阶段名：在导入时选择的目标学科下已存在则复用，不存在则在确认导入时自动创建。</p>
 *
 * <p>题型：单选 / 多选 / 判断 / 简答；正确答案：选择题填 A-F 字母（可用逗号分隔），
 * 判断题填正确/错误，简答题填参考答案全文。</p>
 */
@Service
public class QuestionImportService {

    private static final String STATUS_VALID = "valid";
    private static final String STATUS_WARNING = "warning";
    private static final String STATUS_INVALID = "invalid";
    private static final List<String> OPTION_KEYS = List.of("A", "B", "C", "D", "E", "F");
    private static final Pattern OPTION_HEADER = Pattern.compile("^选项([a-f])$");
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> EXCEL_EXTENSIONS = Set.of("xlsx", "xls");

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final StageMapper stageMapper;
    private final SubjectMapper subjectMapper;
    private final SubjectService subjectService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final OperationLogRecorder operationLogRecorder;

    public QuestionImportService(QuestionMapper questionMapper,
                                 QuestionOptionMapper questionOptionMapper,
                                 StageMapper stageMapper,
                                 SubjectMapper subjectMapper,
                                 SubjectService subjectService,
                                 StringRedisTemplate redis,
                                 ObjectMapper objectMapper,
                                 OperationLogRecorder operationLogRecorder) {
        this.questionMapper = questionMapper;
        this.questionOptionMapper = questionOptionMapper;
        this.stageMapper = stageMapper;
        this.subjectMapper = subjectMapper;
        this.subjectService = subjectService;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.operationLogRecorder = operationLogRecorder;
    }

    /** A-Q12 解析 Excel 并返回预览（不入库）。 */
    public QuestionImportPreview preview(MultipartFile file, Long subjectId) {
        validateFile(file);
        Subject subject = subjectMapper.selectById(subjectId);
        if (subject == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "目标学科不存在");
        }
        Map<String, Stage> existingStages = stageMapper.selectList(
                        com.baomidou.mybatisplus.core.toolkit.Wrappers.<Stage>lambdaQuery()
                                .eq(Stage::getSubjectId, subjectId)).stream()
                .collect(Collectors.toMap(Stage::getName, stage -> stage, (a, b) -> a));

        List<QuestionImportRow> rows = readRows(file, existingStages);
        if (rows.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "Excel 中没有可导入的题目数据");
        }

        int validCount = (int) rows.stream().filter(row -> STATUS_VALID.equals(row.status())).count();
        int warningCount = (int) rows.stream().filter(row -> STATUS_WARNING.equals(row.status())).count();
        int invalidCount = (int) rows.stream().filter(row -> STATUS_INVALID.equals(row.status())).count();
        List<String> newStageNames = rows.stream()
                .filter(row -> !STATUS_INVALID.equals(row.status()))
                .filter(QuestionImportRow::newStage)
                .map(QuestionImportRow::stageName)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        String token = null;
        if (validCount + warningCount > 0) {
            token = UUID.randomUUID().toString().replace("-", "");
            try {
                String json = objectMapper.writeValueAsString(
                        new QuestionImportPayload(subject.getId(), rows));
                redis.opsForValue().set(RedisKeys.QUESTION_IMPORT_PREVIEW + token, json,
                        Duration.ofMinutes(AppConstants.IMPORT_PREVIEW_TTL_MINUTES));
            } catch (Exception ex) {
                throw new BizException(ErrorCode.IMPORT_INVALID, "导入预览保存失败，请重试");
            }
        }

        return new QuestionImportPreview(token, subject.getId(), subject.getName(),
                rows.size(), validCount, warningCount, invalidCount, newStageNames, rows);
    }

    /** A-Q13 确认导入：按需创建阶段，题目统一落为「待审核」。 */
    @Transactional
    public QuestionImportResult confirm(String token) {
        String key = RedisKeys.QUESTION_IMPORT_PREVIEW + token;
        String json = redis.opsForValue().get(key);
        if (json == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "导入预览已失效，请重新上传并解析");
        }

        QuestionImportPayload payload;
        try {
            payload = objectMapper.readValue(json, QuestionImportPayload.class);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "导入数据已损坏，请重新解析");
        }
        List<QuestionImportRow> importable = payload.rows().stream()
                .filter(row -> !STATUS_INVALID.equals(row.status()))
                .toList();
        if (importable.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_INVALID, "没有可导入的有效行");
        }

        Map<String, Long> stageIds = new HashMap<>();
        for (QuestionImportRow row : importable) {
            if (!row.newStage() && row.stageId() != null) {
                stageIds.put(row.stageName(), row.stageId());
            }
        }
        Set<String> newStageNames = importable.stream()
                .filter(QuestionImportRow::newStage)
                .map(QuestionImportRow::stageName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        int createdStages = 0;
        for (String stageName : newStageNames) {
            stageIds.put(stageName, subjectService.findOrCreateStage(payload.subjectId(), stageName));
            createdStages++;
        }

        int imported = 0;
        try {
            for (QuestionImportRow row : importable) {
                Long stageId = stageIds.get(row.stageName());
                if (stageId == null) {
                    throw new BizException(ErrorCode.IMPORT_INVALID,
                            "阶段解析失败：" + row.stageName());
                }
                Question question = new Question();
                question.setStageId(stageId);
                question.setType(row.type());
                question.setDifficulty(row.difficulty());
                question.setContent(row.content());
                question.setAnswer(row.answer());
                question.setAnalysis(row.analysis());
                question.setTags(row.tags());
                question.setStatus(QuestionService.STATUS_PENDING);
                questionMapper.insert(question);

                for (QuestionImportOption option : row.options()) {
                    QuestionOption entity = new QuestionOption();
                    entity.setQuestionId(question.getId());
                    entity.setOptionKey(option.key());
                    entity.setOptionText(option.text());
                    entity.setIsCorrect(option.correct() ? 1 : 0);
                    entity.setSortOrder(OPTION_KEYS.indexOf(option.key()) + 1);
                    questionOptionMapper.insert(entity);
                }
                imported++;
            }
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.NAME_EXISTS, "导入数据冲突，请重新解析后再试");
        }

        redis.delete(key);
        operationLogRecorder.record(OperationActions.CREATE, "题目",
                "批量导入 " + imported + " 道题");
        return new QuestionImportResult(imported, createdStages);
    }

    private List<QuestionImportRow> readRows(MultipartFile file, Map<String, Stage> existingStages) {
        QuestionSheetListener listener = new QuestionSheetListener(existingStages);
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

    /**
     * 多工作表监听器：每个 Sheet 的第一行作为表头，后续行解析为题目。
     */
    private final class QuestionSheetListener extends AnalysisEventListener<Map<Integer, String>> {

        private final Map<String, Stage> existingStages;
        private final List<QuestionImportRow> rows = new ArrayList<>();
        private final Map<Integer, Columns> columnsBySheet = new HashMap<>();
        private int dataRowCount = 0;

        private QuestionSheetListener(Map<String, Stage> existingStages) {
            this.existingStages = existingStages;
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
            rows.add(parseRow(sheetNo, sheetName, rowNumber, data, columns, existingStages));
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // 所有行已在 invoke 中收集
        }

        private List<QuestionImportRow> getRows() {
            return rows;
        }
    }

    private Columns resolveColumns(Map<Integer, String> header, String sheetName) {
        Integer content = findColumn(header, "题干", "题目");
        if (content == null) {
            // 非题目工作表（如说明页）直接跳过
            return null;
        }
        Integer stage = findColumn(header, "分类", "阶段");
        Integer type = findColumn(header, "题型", "类型");
        Integer difficulty = findColumn(header, "难度");
        List<String> missing = new ArrayList<>();
        if (stage == null) {
            missing.add("分类");
        }
        if (type == null) {
            missing.add("题型");
        }
        if (difficulty == null) {
            missing.add("难度");
        }
        if (!missing.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_INVALID,
                    "工作表「" + sheetName + "」缺少表头：" + String.join("、", missing));
        }

        Map<String, Integer> options = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : header.entrySet()) {
            String normalized = normalizeHeader(entry.getValue());
            Matcher matcher = OPTION_HEADER.matcher(normalized);
            if (matcher.matches()) {
                options.put(matcher.group(1).toUpperCase(Locale.ROOT), entry.getKey());
            }
        }
        Integer answer = findColumn(header, "正确答案");
        if (answer == null) {
            answer = findColumn(header, "答案");
        }
        return new Columns(content, stage, type, difficulty, options, answer,
                findColumn(header, "解析"), findColumn(header, "标签", "知识点"));
    }

    private QuestionImportRow parseRow(int sheetNo, String sheetName, int rowNumber,
                                       Map<Integer, String> data, Columns columns,
                                       Map<String, Stage> existingStages) {
        RowBuilder builder = new RowBuilder(sheetNo, sheetName, rowNumber);

        builder.content = cell(data, columns.content);
        if (builder.content == null) {
            builder.error("题干", "不能为空");
        } else if (builder.content.length() > 10000) {
            builder.error("题干", "不能超过 10000 个字符");
        }

        String stageName = cell(data, columns.stage);
        builder.stageName = stageName;
        if (stageName == null) {
            builder.error("分类", "不能为空");
        } else if (stageName.length() > 50) {
            builder.error("分类", "不能超过 50 个字符");
        }

        Integer type = parseType(cell(data, columns.type));
        builder.type = type;
        if (type == null) {
            builder.error("题型", "仅支持单选 / 多选 / 判断 / 简答");
        }

        Integer difficulty = parseDifficulty(cell(data, columns.difficulty));
        builder.difficulty = difficulty;
        if (difficulty == null) {
            builder.error("难度", "仅支持简单 / 中等 / 困难");
        }

        builder.analysis = stripAnalysisPrefix(cell(data, columns.analysis));
        if (builder.analysis != null && builder.analysis.length() > 5000) {
            builder.error("解析", "不能超过 5000 个字符");
        }

        builder.tags = cell(data, columns.tags);
        if (builder.tags != null && builder.tags.length() > 255) {
            builder.error("标签", "不能超过 255 个字符");
        }

        Map<String, String> optionTexts = new LinkedHashMap<>();
        for (String key : OPTION_KEYS) {
            Integer column = columns.options.get(key);
            if (column == null) {
                continue;
            }
            String text = cell(data, column);
            if (text == null) {
                continue;
            }
            if (text.length() > 500) {
                builder.error("选项" + key, "不能超过 500 个字符");
            }
            optionTexts.put(key, text);
        }

        String answerRaw = cell(data, columns.answer);
        if (type != null) {
            if (type == QuestionService.TYPE_SINGLE || type == QuestionService.TYPE_MULTI) {
                if (optionTexts.size() < 2) {
                    builder.error("选项", "选择题至少需要 2 个选项");
                }
                List<String> answerKeys = parseAnswerKeys(answerRaw);
                if (answerRaw == null) {
                    builder.error("正确答案", "不能为空");
                } else if (answerKeys == null) {
                    builder.error("正确答案", "仅支持 A-F 字母，可用逗号分隔");
                } else if (answerKeys.isEmpty()) {
                    builder.error("正确答案", "不能为空");
                } else {
                    if (type == QuestionService.TYPE_SINGLE && answerKeys.size() != 1) {
                        builder.error("正确答案", "单选题只能有一个正确答案");
                    }
                    for (String key : answerKeys) {
                        if (!optionTexts.containsKey(key)) {
                            builder.error("正确答案", "选项 " + key + " 为空或不存在");
                        }
                    }
                }
                builder.answerKeys = answerKeys == null ? List.of() : answerKeys;
                List<QuestionImportOption> options = new ArrayList<>();
                for (Map.Entry<String, String> entry : optionTexts.entrySet()) {
                    options.add(new QuestionImportOption(entry.getKey(), entry.getValue(),
                            builder.answerKeys.contains(entry.getKey())));
                }
                builder.options = options;
                builder.answer = null;
            } else if (type == QuestionService.TYPE_JUDGE) {
                String answer = QuestionService.normalizeJudgeAnswer(answerRaw);
                if (answer == null) {
                    builder.error("正确答案", "判断题答案须为「正确」或「错误」");
                }
                builder.answer = answer;
            } else if (type == QuestionService.TYPE_SHORT) {
                if (answerRaw == null) {
                    builder.error("正确答案", "简答题必须填写参考答案");
                } else if (answerRaw.length() > 10000) {
                    builder.error("正确答案", "不能超过 10000 个字符");
                }
                builder.answer = answerRaw;
            }
        }

        if (stageName != null) {
            Stage existing = existingStages.get(stageName);
            if (existing == null) {
                builder.newStage = true;
            } else {
                builder.stageId = existing.getId();
            }
        }
        return builder.toRow();
    }

    private static Integer parseType(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.replaceAll("\\s+", "");
        if (value.contains("多选")) {
            return QuestionService.TYPE_MULTI;
        }
        if (value.contains("单选")) {
            return QuestionService.TYPE_SINGLE;
        }
        if (value.contains("判断")) {
            return QuestionService.TYPE_JUDGE;
        }
        if (value.contains("简答")) {
            return QuestionService.TYPE_SHORT;
        }
        return null;
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

    /** 正确答案：支持 A,B,C / A、B / ABC 等写法；非法字符返回 null。 */
    private static List<String> parseAnswerKeys(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.toUpperCase(Locale.ROOT).replaceAll("[,，、;；\\s/]+", "");
        if (cleaned.isEmpty()) {
            return List.of();
        }
        if (!cleaned.matches("[A-F]+")) {
            return null;
        }
        return cleaned.chars()
                .mapToObj(value -> String.valueOf((char) value))
                .distinct()
                .toList();
    }

    private static String stripAnalysisPrefix(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.startsWith("解析：")) {
            value = value.substring(3);
        } else if (value.startsWith("解析:")) {
            value = value.substring(3);
        }
        return value.isBlank() ? null : value.trim();
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

    private record Columns(Integer content, Integer stage, Integer type, Integer difficulty,
                           Map<String, Integer> options, Integer answer,
                           Integer analysis, Integer tags) {
    }

    private static final class RowBuilder {

        private final int sheetNo;
        private final String sheetName;
        private final int row;
        private final List<QuestionImportIssue> errors = new ArrayList<>();
        private final List<QuestionImportIssue> warnings = new ArrayList<>();
        private String content;
        private String stageName;
        private Long stageId;
        private boolean newStage;
        private Integer type;
        private Integer difficulty;
        private List<QuestionImportOption> options = List.of();
        private List<String> answerKeys = List.of();
        private String answer;
        private String analysis;
        private String tags;

        private RowBuilder(int sheetNo, String sheetName, int row) {
            this.sheetNo = sheetNo;
            this.sheetName = sheetName;
            this.row = row;
        }

        private void error(String field, String reason) {
            errors.add(new QuestionImportIssue(field, reason));
        }

        private QuestionImportRow toRow() {
            String status;
            if (!errors.isEmpty()) {
                status = STATUS_INVALID;
            } else if (!warnings.isEmpty()) {
                status = STATUS_WARNING;
            } else {
                status = STATUS_VALID;
            }
            return new QuestionImportRow(sheetNo, sheetName, row, content, stageName, stageId,
                    newStage, type, difficulty, options, answerKeys, answer, analysis, tags,
                    status, List.copyOf(errors), List.copyOf(warnings));
        }
    }
}

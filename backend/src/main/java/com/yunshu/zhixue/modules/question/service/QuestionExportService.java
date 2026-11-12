package com.yunshu.zhixue.modules.question.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.question.entity.Question;
import com.yunshu.zhixue.modules.question.entity.QuestionOption;
import com.yunshu.zhixue.modules.question.mapper.QuestionMapper;
import com.yunshu.zhixue.modules.question.mapper.QuestionOptionMapper;
import com.yunshu.zhixue.modules.subject.entity.Stage;
import com.yunshu.zhixue.modules.subject.mapper.StageMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogRecorder;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 题库 Excel 导出 / 导入模板下载（A-Q14、A-Q15）。
 */
@Service
public class QuestionExportService {

    private static final int EXPORT_MAX_ROWS = 10000;
    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final StageMapper stageMapper;
    private final OperationLogRecorder operationLogRecorder;

    public QuestionExportService(QuestionMapper questionMapper,
                                 QuestionOptionMapper questionOptionMapper,
                                 StageMapper stageMapper,
                                 OperationLogRecorder operationLogRecorder) {
        this.questionMapper = questionMapper;
        this.questionOptionMapper = questionOptionMapper;
        this.stageMapper = stageMapper;
        this.operationLogRecorder = operationLogRecorder;
    }

    /** A-Q15 按筛选条件同步导出题库 Excel（上限 1 万条）。 */
    public void export(HttpServletResponse response, String keyword, Long subjectId, Long stageId,
                       Integer type, Integer difficulty, Integer status, String sort) {
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

        long total = questionMapper.selectCount(query);
        if (total > EXPORT_MAX_ROWS) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "导出数量超过 " + EXPORT_MAX_ROWS + " 条，请缩小筛选范围");
        }
        List<Question> questions = questionMapper.selectList(query);
        List<Stage> stages = questions.isEmpty() ? List.of()
                : stageMapper.selectBatchIds(questions.stream()
                .map(Question::getStageId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> stageNames = stages.stream()
                .collect(Collectors.toMap(Stage::getId, Stage::getName, (a, b) -> a));
        Map<Long, List<QuestionOption>> optionsByQuestion = questions.isEmpty() ? Map.of()
                : questionOptionMapper.selectList(Wrappers.<QuestionOption>lambdaQuery()
                        .in(QuestionOption::getQuestionId, questions.stream().map(Question::getId).toList())
                        .orderByAsc(QuestionOption::getSortOrder)
                        .orderByAsc(QuestionOption::getId)).stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));

        List<List<String>> rows = new ArrayList<>();
        for (Question question : questions) {
            List<String> row = new ArrayList<>();
            row.add(question.getContent());
            row.add(stageNames.getOrDefault(question.getStageId(), ""));
            row.add(typeLabel(question.getType()));
            row.add(difficultyLabel(question.getDifficulty()));
            Map<String, String> optionTexts = new HashMap<>();
            List<String> correctKeys = new ArrayList<>();
            for (QuestionOption option : optionsByQuestion.getOrDefault(question.getId(), List.of())) {
                optionTexts.put(option.getOptionKey(), option.getOptionText());
                if (Integer.valueOf(1).equals(option.getIsCorrect())) {
                    correctKeys.add(option.getOptionKey());
                }
            }
            for (String key : List.of("A", "B", "C", "D", "E", "F")) {
                row.add(optionTexts.getOrDefault(key, ""));
            }
            row.add(question.getType() != null && (question.getType() == 1 || question.getType() == 2)
                    ? String.join(",", correctKeys)
                    : nullToEmpty(question.getAnswer()));
            row.add(nullToEmpty(question.getAnalysis()));
            row.add(nullToEmpty(question.getTags()));
            rows.add(row);
        }

        writeWorkbook(response, "题库导出.xlsx", "题库", rows);
        operationLogRecorder.record(OperationActions.EXPORT, "题目", "导出题库 " + rows.size() + " 道题");
    }

    /** A-Q14 下载导入模板（含一行示例）。 */
    public void template(HttpServletResponse response) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of(
                "示例：以下哪个是 Java 的合法标识符？",
                "AI工程化-基础与项目实战",
                "单选题",
                "简单",
                "_name", "1name", "class", "name-1", "", "",
                "A", "", "JavaSE基础"));
        writeWorkbook(response, "题库导入模板.xlsx", "题库", rows);
    }

    private void writeWorkbook(HttpServletResponse response, String filename, String sheetName,
                               List<List<String>> rows) {
        List<List<String>> head = List.of(
                List.of("题干"), List.of("分类"), List.of("题型"), List.of("难度"),
                List.of("选项A"), List.of("选项B"), List.of("选项C"),
                List.of("选项D"), List.of("选项E"), List.of("选项F"),
                List.of("正确答案"), List.of("解析"), List.of("标签"));
        try {
            response.setContentType(CONTENT_TYPE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                    + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"));
            EasyExcel.write(response.getOutputStream())
                    .head(head)
                    .sheet(sheetName)
                    .doWrite(rows);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "文件生成失败");
        }
    }

    private static String typeLabel(Integer type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case 1 -> "单选题";
            case 2 -> "多选题";
            case 3 -> "判断题";
            case 4 -> "简答题";
            default -> "";
        };
    }

    private static String difficultyLabel(Integer difficulty) {
        if (difficulty == null) {
            return "";
        }
        return switch (difficulty) {
            case 1 -> "简单";
            case 2 -> "中等";
            case 3 -> "困难";
            default -> "";
        };
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

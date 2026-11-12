package com.yunshu.zhixue.modules.question.dto;

import java.util.List;

/**
 * A-Q12 题目 Excel 导入的逐行解析结果。
 *
 * @param sheetName 工作表名，便于多 Sheet 文件定位
 * @param row       Excel 行号（1 起，表头为第 1 行）
 * @param stageName Excel「分类」列，作为所选学科下的阶段名
 * @param newStage  阶段不存在、确认导入时将自动创建
 */
public record QuestionImportRow(
        int sheetNo,
        String sheetName,
        int row,
        String content,
        String stageName,
        Long stageId,
        boolean newStage,
        Integer type,
        Integer difficulty,
        List<QuestionImportOption> options,
        List<String> answerKeys,
        String answer,
        String analysis,
        String tags,
        String status,
        List<QuestionImportIssue> errors,
        List<QuestionImportIssue> warnings) {
}

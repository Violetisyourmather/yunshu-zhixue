package com.yunshu.zhixue.modules.question.dto;

import java.util.List;

/**
 * P-B04 题目详情。
 *
 * <p>未登录时 {@code answer}/{@code analysis} 为 null、{@code inWrongBook} 为 false；
 * 登录后返回答案、解析与本人错题本状态。</p>
 */
public record PortalQuestionDetail(
        long id,
        int type,
        int difficulty,
        String content,
        String subjectName,
        String stageName,
        String tags,
        List<PortalQuestionOptionItem> options,
        String answer,
        String analysis,
        boolean inWrongBook,
        long doneCount,
        double accuracyRate) {
}
